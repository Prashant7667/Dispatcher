package org.dispatchsystem.driver.dto;

import lombok.Builder;
import lombok.Data;
import org.dispatchsystem.driver.domain.VehicleClass;
import org.dispatchsystem.ride.domain.BookingType;
import org.dispatchsystem.ride.domain.RentalPlan;
import org.dispatchsystem.ride.domain.RideStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class DriverRideOfferNotification {
    private Long rideId;
    private Double startLongitude;
    private Double startLatitude;
    private Double endLongitude;
    private Double endLatitude;
    private VehicleClass requestedVehicleClass;
    private Integer requiredLuggageCapacity;
    private RideStatus status;
    private BookingType bookingType;
    private Integer estimatedDurationMinutes;
    private RentalPlan rentalPlan;
    private LocalDateTime createdAt;
    private LocalDateTime scheduledStart;
    private Double fare;
    private Long userId;
    private String userName;
}
