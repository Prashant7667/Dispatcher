package org.dispatchsystem.ride.domain;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.driver.domain.VehicleClass;
import org.dispatchsystem.user.domain.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    @Enumerated(EnumType.STRING)
    private VehicleClass requestedVehicleClass;
    private int requiredLuggageCapacity;
    @Enumerated(EnumType.STRING)
    private RideStatus status = RideStatus.REQUESTED;
    @Enumerated(EnumType.STRING)
    private BookingType bookingType = BookingType.TRIP;
    private LocalDateTime scheduledStart;
    private Integer estimatedDurationMinutes;
    @Enumerated(EnumType.STRING)
    private RentalPlan rentalPlan = RentalPlan.NONE;

    private Double fare;
    @ManyToOne
    private Driver driver;
    @ManyToOne
    private User user;

}
