package org.dispatchsystem.driver.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

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
}
