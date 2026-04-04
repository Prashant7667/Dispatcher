package org.dispatchsystem.driver.service;

import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.service.RideBroadcastService;
import org.springframework.stereotype.Service;

@Service
public class DriverSocketNotificationService {
    private final DriverSocketSender driverSocketSender;
    public DriverSocketNotificationService(DriverSocketSender driverSocketSender){
        this.driverSocketSender=driverSocketSender;
    }
    public void sendRideOffer(Driver driver, Ride ride){
        driverSocketSender.sendOfferToDriver(driver,ride);
    }
}
