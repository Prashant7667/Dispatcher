package org.dispatchsystem.dispatch.offer;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.domain.RideOffer;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
@Data
public class OfferFlowState {
    private final Ride ride;
    private final List<Driver> rankedDrivers;
    private int currentIndex = 0;
    private RideOffer currentOffer;
    private ScheduledFuture<?> timeoutFuture;
    OfferFlowState(Ride ride, List<Driver> rankedDrivers){
        this.ride = ride;
        this.rankedDrivers = rankedDrivers;
    }
    void incrementIndex(){
        currentIndex++;
    }
}
