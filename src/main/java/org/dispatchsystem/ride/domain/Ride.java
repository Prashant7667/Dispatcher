package org.dispatchsystem.ride.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.user.domain.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "rides")
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
    private User user;

    public enum RideStatus {
        REQUESTED,
        ACCEPTED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
}
