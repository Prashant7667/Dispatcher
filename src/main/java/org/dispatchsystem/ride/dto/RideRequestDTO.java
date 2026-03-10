package org.dispatchsystem.ride.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.dispatchsystem.ride.domain.BookingType;
import org.dispatchsystem.ride.domain.RentalPlan;

import java.time.LocalDateTime;

@Data
public class RideRequestDTO {
    @NotNull(message = "Start longitude is required")
    private Double startLongitude;
    @NotNull(message = "Start latitude is required")
    private Double startLatitude;
    @NotNull(message = "End longitude is required")
    private Double endLongitude;
    @NotNull(message = "End latitude is required")
    private Double endLatitude;
    @NotNull(message = "Booking type is required")
    private BookingType bookingType;
    private LocalDateTime scheduledStart;
    @Positive(message = "Estimated duration must be positive")
    private Integer estimatedDurationMinutes;
    private RentalPlan rentalPlan;
    @NotNull(message = "Fare is required")
    @Positive(message = "Fare must be positive")
    private Double fare;
}
