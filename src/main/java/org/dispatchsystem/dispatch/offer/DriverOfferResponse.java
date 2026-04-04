package org.dispatchsystem.dispatch.offer;

import lombok.Data;
import org.dispatchsystem.ride.domain.OfferStatus;

@Data
public class DriverOfferResponse {

    private OfferStatus type;
    private Long rideId;
    private String message;

    public String resolveAction() {
        if (type == OfferStatus.ACCEPTED) {
            return "ACCEPT";
        }
        if (type == OfferStatus.REJECTED) {
            return "REJECT";
        }
        return message;
    }
}
