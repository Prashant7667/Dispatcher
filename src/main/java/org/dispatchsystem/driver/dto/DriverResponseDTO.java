package org.dispatchsystem.driver.dto;

import lombok.Data;
import org.dispatchsystem.driver.domain.AvailabilityStatus;
import org.dispatchsystem.driver.domain.DriverBookingType;
import org.dispatchsystem.driver.domain.VehicleDetails;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class DriverResponseDTO {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private VehicleDetails vehicleDetails;
    private Double latitude;
    private Double longitude;
    private Double avgRating;
    private Long totalRating;
    private Set<DriverBookingType> supportedBookingTypes;
    private LocalDateTime availableFrom;
    private LocalDateTime availableUntil;
    private Integer maxRentalDurationMinutes;
    private AvailabilityStatus availabilityStatus;
}
