package org.dispatchsystem.driver.dto;

import lombok.Data;
import org.dispatchsystem.driver.domain.AvailabilityStatus;

@Data
public class DriverResponseDTO {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private String vehicleDetails;
    private Double latitude;
    private Double longitude;
    private Double avgRating;
    private Long totalRating;
    private AvailabilityStatus availabilityStatus;
}
