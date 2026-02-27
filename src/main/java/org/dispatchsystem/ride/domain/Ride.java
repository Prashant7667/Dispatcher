package org.dispatchsystem.ride.domain;

import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.user.domain.User;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Ride {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private double startLongitude;
    private double startLatitude;
    private double endLongitude;
    private double endLatitude;
    private RideStatus status = RideStatus.REQUESTED;

    private Double fare;
    @ManyToOne
    private Driver driver;
    @ManyToOne
    private User rider;

    public enum RideStatus {
        REQUESTED,
        ACCEPTED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
}
