package org.dispatchsystem.driver.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Driver {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private String password;
    private String phoneNumber;
    private String vehicleDetails;
    private Double latitude;
    private Double longitude;
    private Double AvgRating = 0.0;
    private Long TotalRating = 0L;
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.UNAVAILABLE;

    public enum AvailabilityStatus {
        AVAILABLE,
        UNAVAILABLE
    }
}
