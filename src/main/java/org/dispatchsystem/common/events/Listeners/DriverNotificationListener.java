package org.dispatchsystem.common.events.Listeners;

import org.dispatchsystem.common.events.DriverOfferCreatedEvent;
import org.dispatchsystem.driver.service.DriverSocketNotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DriverNotificationListener {
    private final DriverSocketNotificationService driverSocketNotificationService;
    public DriverNotificationListener(DriverSocketNotificationService driverSocketNotificationService){
        this.driverSocketNotificationService=driverSocketNotificationService;
    }
    @EventListener
    public void onOfferCreatedEvent(DriverOfferCreatedEvent event){
        driverSocketNotificationService.sendRideOffer(event.getDriver(),event.getRide());
    }

}
