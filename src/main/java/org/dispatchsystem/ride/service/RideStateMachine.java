package org.dispatchsystem.ride.service;

import org.dispatchsystem.common.events.RideStatusChangedEvent;
import org.dispatchsystem.common.exceptions.InvalidRideStateException;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.domain.RideStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class RideStateMachine {

    // Define valid transitions
    private static final Map<RideStatus, Set<RideStatus>> VALID_TRANSITIONS = Map.of(
            RideStatus.REQUESTED,       Set.of(RideStatus.DISPATCHING, RideStatus.CANCELLED),
            RideStatus.SCHEDULED,       Set.of(RideStatus.CANCELLED, RideStatus.DISPATCHING),
            RideStatus.DISPATCHING,     Set.of(RideStatus.DRIVER_ASSIGNED, RideStatus.CANCELLED),
            RideStatus.DRIVER_ASSIGNED, Set.of(RideStatus.DRIVER_EN_ROUTE, RideStatus.CANCELLED),
            RideStatus.DRIVER_EN_ROUTE, Set.of(RideStatus.DRIVER_ARRIVED, RideStatus.CANCELLED),
            RideStatus.DRIVER_ARRIVED,  Set.of(RideStatus.IN_PROGRESS, RideStatus.CANCELLED),
            RideStatus.IN_PROGRESS,     Set.of(RideStatus.COMPLETED),
            RideStatus.COMPLETED,       Set.of(),
            RideStatus.CANCELLED,       Set.of()
    );

    private final ApplicationEventPublisher eventPublisher;
    public RideStateMachine(ApplicationEventPublisher eventPublisher){
        this.eventPublisher=eventPublisher;
    }

    /**
     * Transition a ride to a new status.
     * Throws InvalidRideStateException if the transition is not allowed.
     * Publishes RideStatusChangedEvent on success.
     */
    public void transition(Ride ride, RideStatus newStatus) {
        RideStatus oldStatus = ride.getStatus();
        Set<RideStatus> allowed = VALID_TRANSITIONS.get(oldStatus);

        if (allowed == null || !allowed.contains(newStatus)) {
            throw new InvalidRideStateException(
                    "Cannot transition ride " + ride.getId() +
                            " from " + oldStatus + " to " + newStatus
            );
        }

        ride.setStatus(newStatus);
        eventPublisher.publishEvent(new RideStatusChangedEvent(ride, oldStatus, newStatus));
    }
}
