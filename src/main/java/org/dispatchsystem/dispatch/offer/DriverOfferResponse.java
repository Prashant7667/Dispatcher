package org.dispatchsystem.dispatch.offer;

import lombok.Data;
import org.dispatchsystem.ride.domain.OfferStatus;

@Data
public class DriverOfferResponse {

    private OfferStatus type;
    private Long rideId;
    private String message;
}
