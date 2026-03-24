package org.dispatchsystem.driver.dto;

import jakarta.validation.Valid;
import lombok.Data;
import org.dispatchsystem.driver.domain.AvailabilityStatus;
import org.dispatchsystem.driver.domain.DriverBookingType;
import org.dispatchsystem.driver.domain.VehicleDetails;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class DriverUpdateDTO {
    private String name;
    private String password;
    private String phoneNumber;
    @Valid
    private VehicleDetails vehicleDetails;
    private Double latitude;
    private Double longitude;
    private Set<DriverBookingType> supportedBookingTypes;
    private LocalDateTime availableFrom;
    private LocalDateTime availableUntil;
    private Integer maxRentalDurationMinutes;
    private AvailabilityStatus availabilityStatus;
}
