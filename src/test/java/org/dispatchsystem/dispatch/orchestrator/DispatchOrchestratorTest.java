package org.dispatchsystem.dispatch.orchestrator;

import org.dispatchsystem.dispatch.model.DispatchCandidate;
import org.dispatchsystem.dispatch.model.DispatchDecision;
import org.dispatchsystem.dispatch.offer.OfferManager;
import org.dispatchsystem.dispatch.service.DispatchDecisionService;
import org.dispatchsystem.driver.domain.AvailabilityStatus;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.driver.repository.DriverRepository;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.domain.RideStatus;
import org.dispatchsystem.ride.repository.RideRepository;
import org.dispatchsystem.ride.service.DispatchAuditService;
import org.dispatchsystem.ride.service.RideStateMachine;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DispatchOrchestratorTest {

    @Test
    void dispatchStartsOfferFlowWithDriversRankedByDispatchScoreThenDistance() {
        OfferManager offerManager = mock(OfferManager.class);
        RideRepository rideRepository = mock(RideRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        RideStateMachine rideStateMachine = new RideStateMachine(eventPublisher);
        DriverRepository driverRepository = mock(DriverRepository.class);
        DispatchDecisionService dispatchDecisionService = mock(DispatchDecisionService.class);
        DispatchAuditService dispatchAuditService = mock(DispatchAuditService.class);

        DispatchOrchestrator orchestrator = new DispatchOrchestrator(
                offerManager,
                rideRepository,
                rideStateMachine,
                driverRepository,
                dispatchDecisionService,
                dispatchAuditService
        );

        Ride ride = new Ride();
        ride.setId(301L);
        ride.setStatus(RideStatus.REQUESTED);

        Driver fartherDriver = new Driver();
        fartherDriver.setId(11L);
        fartherDriver.setEmail("far@dispatch.dev");
        Driver closerDriver = new Driver();
        closerDriver.setId(12L);
        closerDriver.setEmail("near@dispatch.dev");
        Driver bestScoreDriver = new Driver();
        bestScoreDriver.setId(13L);
        bestScoreDriver.setEmail("best@dispatch.dev");

        DispatchDecision decision = new DispatchDecision(
                ride,
                List.of(
                        new DispatchCandidate(fartherDriver, 1.6, true, List.of(), List.of(), 8.0, new DispatchCandidate.ScoreBreakdown(4.0, 2.4, 1.6)),
                        new DispatchCandidate(closerDriver, 1.2, true, List.of(), List.of(), 8.0, new DispatchCandidate.ScoreBreakdown(4.0, 2.8, 1.2)),
                        new DispatchCandidate(bestScoreDriver, 2.1, true, List.of(), List.of(), 9.5, new DispatchCandidate.ScoreBreakdown(4.5, 2.9, 2.1))
                ),
                List.of()
        );

        when(driverRepository.findByAvailabilityStatus(AvailabilityStatus.AVAILABLE))
                .thenReturn(List.of(fartherDriver, closerDriver, bestScoreDriver));
        when(dispatchDecisionService.takeDecision(ride, List.of(fartherDriver, closerDriver, bestScoreDriver)))
                .thenReturn(decision);
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.dispatch(ride);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Driver>> rankedDriversCaptor =
                (ArgumentCaptor<List<Driver>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
        verify(offerManager).startOfferFlow(any(Ride.class), rankedDriversCaptor.capture());
        assertEquals(List.of(bestScoreDriver, closerDriver, fartherDriver), rankedDriversCaptor.getValue());
        assertEquals(RideStatus.DISPATCHING, ride.getStatus());
    }

    @Test
    void dispatchCancelsRideWhenNoEligibleDriversExist() {
        OfferManager offerManager = mock(OfferManager.class);
        RideRepository rideRepository = mock(RideRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        RideStateMachine rideStateMachine = new RideStateMachine(eventPublisher);
        DriverRepository driverRepository = mock(DriverRepository.class);
        DispatchDecisionService dispatchDecisionService = mock(DispatchDecisionService.class);
        DispatchAuditService dispatchAuditService = mock(DispatchAuditService.class);

        DispatchOrchestrator orchestrator = new DispatchOrchestrator(
                offerManager,
                rideRepository,
                rideStateMachine,
                driverRepository,
                dispatchDecisionService,
                dispatchAuditService
        );

        Ride ride = new Ride();
        ride.setId(302L);
        ride.setStatus(RideStatus.REQUESTED);

        when(driverRepository.findByAvailabilityStatus(AvailabilityStatus.AVAILABLE)).thenReturn(List.of());
        when(dispatchDecisionService.takeDecision(ride, List.of()))
                .thenReturn(new DispatchDecision(ride, List.of(), List.of()));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.dispatch(ride);

        assertEquals(RideStatus.CANCELLED, ride.getStatus());
        verify(offerManager, never()).startOfferFlow(any(Ride.class), any());
    }
}
