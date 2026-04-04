package org.dispatchsystem.ride.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DispatchAnalyticsSummaryDTO {
    private long totalRides;
    private long totalOffersSent;
    private long acceptedOffers;
    private long rejectedOffers;
    private long timedOutOffers;
    private long cancelledRides;
    private double acceptanceRate;
    private double rejectionRate;
    private double timeoutRate;
    private double cancellationRate;
    private double averageDispatchTimeSeconds;
    private double driverUtilizationRate;
}
