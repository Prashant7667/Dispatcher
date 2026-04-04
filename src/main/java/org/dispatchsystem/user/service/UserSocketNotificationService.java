package org.dispatchsystem.user.service;

import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.user.domain.UserNotificationType;
import org.dispatchsystem.user.domain.UserRideNotification;
import org.springframework.stereotype.Service;

@Service
public class UserSocketNotificationService {
    private final UserSocketSender userSocketSender;

    public UserSocketNotificationService(UserSocketSender userSocketSender){
        this.userSocketSender=userSocketSender;
    }

    public void notifyDriverAssigned(Ride ride){
        userSocketSender.sendToUser(buildNotification(
                UserNotificationType.DRIVER_ASSIGNED,
                ride,
                ride.getDriver() != null && ride.getDriver().getName() != null
                        ? "Driver " + ride.getDriver().getName() + " has been assigned to your ride"
                        : "A driver has been assigned to your ride"
        ));
    }

    public void notifyNoDriversAvailble(Ride ride){
        userSocketSender.sendToUser(buildNotification(
                UserNotificationType.NO_DRIVERS_AVAILABLE,
                ride,
                "No drivers are available right now"
        ));
    }

    public void notifyDriverEnRoute(Ride ride){
        userSocketSender.sendToUser(buildNotification(
                UserNotificationType.DRIVER_EN_ROUTE,
                ride,
                "Your driver is on the way"
        ));
    }

    public void notifyDriverArrived(Ride ride){
        userSocketSender.sendToUser(buildNotification(
                UserNotificationType.DRIVER_ARRIVED,
                ride,
                "Your driver has arrived"
        ));
    }

    public void notifyRideCancelled(Ride ride){
        userSocketSender.sendToUser(buildNotification(
                UserNotificationType.RIDE_CANCELLED,
                ride,
                "Your ride has been cancelled"
        ));
    }

    private UserRideNotification buildNotification(UserNotificationType type, Ride ride, String message) {
        UserRideNotification notification = new UserRideNotification();
        notification.setType(type);
        notification.setMessage(message);
        notification.setRideId(ride.getId());
        notification.setRideStatus(ride.getStatus());
        notification.setDriverId(ride.getDriver() != null ? ride.getDriver().getId() : null);
        notification.setDriverName(ride.getDriver() != null ? ride.getDriver().getName() : null);
        notification.setRide(ride);
        return notification;
    }
}
