package org.dispatchsystem.driver.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dispatchsystem.driver.domain.DriverBookingType;
import org.dispatchsystem.driver.domain.VehicleDetails;

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
    @NotNull(message = "vehicleDetails is required")
    @Valid
    private VehicleDetails vehicleDetails;
    @NotNull(message = "latitude is required")
    private Double latitude;
    @NotNull(message = "longitude is required")
    private Double longitude;
    private Set<DriverBookingType> supportedBookingTypes;
    private LocalDateTime availableFrom;
    private LocalDateTime availableUntil;
    private Integer maxRentalDurationMinutes;
}
