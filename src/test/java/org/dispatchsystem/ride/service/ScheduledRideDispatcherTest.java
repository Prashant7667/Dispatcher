package org.dispatchsystem.ride.service;

import org.dispatchsystem.dispatch.orchestrator.DispatchOrchestrator;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.domain.RideStatus;
import org.dispatchsystem.ride.repository.RideRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledRideDispatcherTest {

    @Test
    void dispatchScheduledRidesDispatchesDueScheduledRides() {
        RideRepository rideRepository = mock(RideRepository.class);
        DispatchOrchestrator dispatchOrchestrator = mock(DispatchOrchestrator.class);
        ScheduledRideDispatcher scheduledRideDispatcher = new ScheduledRideDispatcher(rideRepository, dispatchOrchestrator);

        Ride scheduledRide = new Ride();
        scheduledRide.setId(401L);
        scheduledRide.setStatus(RideStatus.SCHEDULED);
        scheduledRide.setScheduledStart(LocalDateTime.now().plusMinutes(3));

        when(rideRepository.findByStatusAndScheduledStartLessThanEqual(eq(RideStatus.SCHEDULED), any(LocalDateTime.class)))
                .thenReturn(List.of(scheduledRide));

        scheduledRideDispatcher.dispatchScheduledRides();

        verify(dispatchOrchestrator).dispatch(scheduledRide);
    }
}
