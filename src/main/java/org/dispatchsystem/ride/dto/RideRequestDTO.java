package org.dispatchsystem.ride.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.dispatchsystem.driver.domain.VehicleClass;
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
    @NotNull(message = "requestedVehicleClass is required")
    private VehicleClass requestedVehicleClass;
    @NotNull(message = "requiredLuggageCapacity is required")
    @PositiveOrZero(message = "requiredLuggageCapacity must be zero or positive")
    private Integer requiredLuggageCapacity;
}
