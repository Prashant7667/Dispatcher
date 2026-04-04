package org.dispatchsystem.user.dto.notification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.dispatchsystem.ride.domain.RideStatus;

@Data
public class UserRideNotification {
    private UserNotificationType type;
    private String message;
    @JsonIgnore
    private String recipientEmail;
    private Long rideId;
    private RideStatus rideStatus;
    private String driverName;
    private Long driverId;
    private UserRideSnapshotDTO ride;
}
