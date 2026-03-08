package org.dispatchsystem.ride.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class RideUpdateDTO {
    @NotNull(message = "Start longitude is required")
    private Double startLongitude;
    @NotNull(message = "Start latitude is required")
    private Double startLatitude;
    @NotNull(message = "End longitude is required")
    private Double endLongitude;
    @NotNull(message = "End latitude is required")
    private Double endLatitude;
    @NotNull(message = "Fare is required")
    @Positive(message = "Fare must be positive")
    private Double fare;
}
