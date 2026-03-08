package org.dispatchsystem.common.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.dispatchsystem.ride.domain.Ride;
@Data
@AllArgsConstructor
public class NoDriversAvailableEvent {
    public Ride ride;
}
