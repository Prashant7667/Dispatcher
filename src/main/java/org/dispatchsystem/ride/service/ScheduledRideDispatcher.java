package org.dispatchsystem.ride.service;

import org.dispatchsystem.dispatch.orchestrator.DispatchOrchestrator;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.domain.RideStatus;
import org.dispatchsystem.ride.repository.RideRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScheduledRideDispatcher {
    private final RideRepository rideRepository;
    private final DispatchOrchestrator dispatchOrchestrator;
    public ScheduledRideDispatcher(RideRepository rideRepository, DispatchOrchestrator dispatchOrchestrator){
        this.rideRepository=rideRepository;
        this.dispatchOrchestrator=dispatchOrchestrator;
    }
    @Scheduled(fixedDelay = 100000)
    public void dispatchScheduledRides(){
        LocalDateTime dispatchThreshold = LocalDateTime.now().plusMinutes(5);
        List<Ride> dueRides = rideRepository.findByStatusAndScheduledStartLessThanEqual(RideStatus.SCHEDULED,dispatchThreshold );
        for(Ride ride:dueRides){
            dispatchOrchestrator.dispatch(ride);
        }
    }
}
