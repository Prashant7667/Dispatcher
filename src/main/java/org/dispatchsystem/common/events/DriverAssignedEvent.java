package org.dispatchsystem.common.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.ride.domain.Ride;
@Data
@AllArgsConstructor
public class DriverAssignedEvent {
    private Driver driver;
    private Ride ride;

}
