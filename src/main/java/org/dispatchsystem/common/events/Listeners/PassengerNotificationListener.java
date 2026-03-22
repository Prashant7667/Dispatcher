package org.dispatchsystem.common.events.Listeners;

import org.dispatchsystem.common.events.DriverAssignedEvent;
import org.dispatchsystem.common.events.NoDriversAvailableEvent;
import org.dispatchsystem.common.events.RideCancelledEvent;
import org.dispatchsystem.common.events.RideStatusChangedEvent;
import org.dispatchsystem.ride.domain.RideStatus;
import org.dispatchsystem.user.service.UserSocketNotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PassengerNotificationListener {
    private final UserSocketNotificationService userSocketNotificationService;
    public PassengerNotificationListener(UserSocketNotificationService userSocketNotificationService){
        this.userSocketNotificationService = userSocketNotificationService;
    }
    @EventListener
    public void onDriverAssigned(DriverAssignedEvent driverAssignedEvent){
        userSocketNotificationService.notifyDriverAssigned(driverAssignedEvent.getRide());
    }
    @EventListener
    public void onNoDrivers(NoDriversAvailableEvent noDriversAvailableEvent) {
        // Future: notify rider, suggest retry
        userSocketNotificationService.notifyNoDriversAvailble(noDriversAvailableEvent.getRide());
    }
    @EventListener
    public void onRideStatusChanged(RideStatusChangedEvent rideStatusChangedEvent){
        if(rideStatusChangedEvent.getNewStatus()== RideStatus.DRIVER_ARRIVED){
            userSocketNotificationService.notifyDriverArrived(rideStatusChangedEvent.getRide());
        }
        if(rideStatusChangedEvent.getNewStatus()==RideStatus.DRIVER_EN_ROUTE){
            userSocketNotificationService.notifyDriverEnRoute(rideStatusChangedEvent.getRide());
        }

    }
    @EventListener
    public void onRideCancelled(RideCancelledEvent rideCancelledEvent){
        userSocketNotificationService.notifyRideCancelled(rideCancelledEvent.getRide());
    }

}
