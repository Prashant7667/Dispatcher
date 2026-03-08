package org.dispatchsystem.ride.dto;

import lombok.Data;
import org.dispatchsystem.ride.domain.RideStatus;

@Data
public class RideResponseDTO {
    private Long id;
    private Double startLongitude;
    private Double startLatitude;
    private Double endLongitude;
    private Double endLatitude;
    private RideStatus status;
    private Double fare;
    private Long driverId;
    private String driverName;
    private Long userId;
    private String userName;
}
