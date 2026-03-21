package org.dispatchsystem.driver.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.dispatchsystem.driver.domain.DriverBookingType;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class DriverRequestDTO {
    @NotBlank(message = "name is required")
    private String name;
    @NotBlank(message = "email is required")
    private String email;
    @NotBlank(message = "password is required")
    private String password;
    @NotBlank(message = "phoneNumber is required")
    private String phoneNumber;
    private String vehicleDetails;
    private Double latitude;
    private Double longitude;
    private Set<DriverBookingType> supportedBookingTypes;
    private LocalDateTime availableFrom;
    private LocalDateTime availableUntil;
    private Integer maxRentalDurationMinutes;
}
