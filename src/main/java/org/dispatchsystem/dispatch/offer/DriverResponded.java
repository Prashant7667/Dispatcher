package org.dispatchsystem.dispatch.offer;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DriverResponded {
    private OfferStatusState offerStatus;
    private Long rideId;
    private String message;
}
