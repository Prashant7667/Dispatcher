package org.dispatchsystem.driver.dto;

import lombok.Data;
import org.dispatchsystem.driver.domain.AvailabilityStatus;

@Data
public class DriverUpdateDTO {
    private String name;
    private String password;
    private String phoneNumber;
    private String vehicleDetails;
    private Double latitude;
    private Double longitude;
    private AvailabilityStatus availabilityStatus;
}
