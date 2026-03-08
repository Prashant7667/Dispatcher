package org.dispatchsystem.common.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.dispatchsystem.ride.domain.Ride;
@Data
@AllArgsConstructor
public class RideCancelledEvent {
    private Ride ride;
}
