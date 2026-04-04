package org.dispatchsystem.dispatch.orchestrator;

import org.dispatchsystem.common.events.domains.ReasonCode;
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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DispatchOrchestrator {
    private final OfferManager offerManager;
    private final RideRepository rideRepository;
    private final RideStateMachine rideStateMachine;
    private final DriverRepository driverRepository;
    private final DispatchDecisionService dispatchDecisionService;
    private final DispatchAuditService dispatchAuditService;
    DispatchOrchestrator(OfferManager offerManager, RideRepository rideRepository, RideStateMachine rideStateMachine, DriverRepository driverRepository, DispatchDecisionService dispatchDecisionService, DispatchAuditService dispatchAuditService){
        this.offerManager = offerManager;
        this.rideRepository = rideRepository;
        this.rideStateMachine = rideStateMachine;
        this.driverRepository = driverRepository;
        this.dispatchDecisionService = dispatchDecisionService;
        this.dispatchAuditService = dispatchAuditService;
    }
    public void dispatch(Ride ride){
        ride.setDispatchStartedAt(LocalDateTime.now());
        dispatchAuditService.recordDispatchStarted(ride);
        List<Driver> availableDrivers= driverRepository.findByAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        DispatchDecision decision= dispatchDecisionService.takeDecision(ride, availableDrivers);
        Map<Long, Integer> acceptedRanks = new HashMap<>();
        List<Driver> rankedDrivers = decision.rankedDrivers();
        for (int i = 0; i < rankedDrivers.size(); i++) {
            acceptedRanks.put(rankedDrivers.get(i).getId(), i + 1);
        }
        for (var candidate : decision.getAcceptedCandidates()) {
            dispatchAuditService.recordDriverEvaluation(ride, candidate, acceptedRanks.get(candidate.getDriver().getId()));
        }
        for (var candidate : decision.getRejectedCandidates()) {
            dispatchAuditService.recordDriverEvaluation(ride, candidate, null);
        }
        if(!decision.hasEligibleCandidates()){
            ride.setCancelledAt(LocalDateTime.now());
            dispatchAuditService.recordDispatchFailed(ride, ReasonCode.NO_ELIGIBLE_DRIVERS, "Dispatch failed because no eligible drivers were available");
            rideStateMachine.transition(ride, RideStatus.CANCELLED);
            rideRepository.save(ride);
            // TODO: notify rider "no drivers available"
            return;
        }
        // Step 3: Start the offer flow — try drivers one by one
        rideStateMachine.transition(ride, RideStatus.DISPATCHING);
        rideRepository.save(ride);
        offerManager.startOfferFlow(ride, rankedDrivers);
    }
}
