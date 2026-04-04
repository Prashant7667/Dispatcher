package org.dispatchsystem.user.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.domain.RideStatus;
@Data
public class UserRideNotification {
        private UserNotificationType type;
        private String message;
        private Long rideId;
        private RideStatus rideStatus;
        private String driverName;
        private Long driverId;
        private Ride ride;

}
