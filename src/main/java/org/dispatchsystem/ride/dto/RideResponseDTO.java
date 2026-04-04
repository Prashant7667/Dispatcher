package org.dispatchsystem.ride.dto;

import lombok.Data;
import org.dispatchsystem.driver.domain.VehicleClass;
import org.dispatchsystem.ride.domain.BookingType;
import org.dispatchsystem.ride.domain.RentalPlan;
import org.dispatchsystem.ride.domain.RideStatus;

import java.time.LocalDateTime;

@Data
public class RideResponseDTO {
    private Long id;
    private Double startLongitude;
    private Double startLatitude;
    private Double endLongitude;
    private Double endLatitude;
    private BookingType bookingType;
    private LocalDateTime scheduledStart;
    private Integer estimatedDurationMinutes;
    private RentalPlan rentalPlan;
    private RideStatus status;
    private Double fare;
    private Long driverId;
    private String driverName;
    private Long userId;
    private String userName;
    private VehicleClass requestedVehicleClass;
    private int requiredLuggageCapacity;
}
