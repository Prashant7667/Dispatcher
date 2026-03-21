package org.dispatchsystem.driver.dto;

import lombok.Data;
import org.dispatchsystem.driver.domain.AvailabilityStatus;
import org.dispatchsystem.driver.domain.DriverBookingType;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class DriverUpdateDTO {
    private String name;
    private String password;
    private String phoneNumber;
    private String vehicleDetails;
    private Double latitude;
    private Double longitude;
    private Set<DriverBookingType> supportedBookingTypes;
    private LocalDateTime availableFrom;
    private LocalDateTime availableUntil;
    private Integer maxRentalDurationMinutes;
    private AvailabilityStatus availabilityStatus;
}
