package org.dispatchsystem.common.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.domain.RideStatus;
@Data
@AllArgsConstructor
public class RideStatusChangedEvent {
    private Ride ride;
    private RideStatus oldStatus;
    private RideStatus newStatus;
}
