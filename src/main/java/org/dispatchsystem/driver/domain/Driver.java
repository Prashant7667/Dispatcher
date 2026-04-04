package org.dispatchsystem.driver.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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
    private Double avgRating = 0.0;
    private Long totalRating = 0L;
    @ElementCollection(targetClass = DriverBookingType.class, fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "driver_supported_booking_types", joinColumns = @JoinColumn(name = "driver_id"))
    @Column(name = "booking_type")
    private Set<DriverBookingType> supportedBookingTypes = new HashSet<>();
    private LocalDateTime availableFrom;
    private LocalDateTime availableUntil;
    private Integer maxRentalDurationMinutes;
    @Enumerated(EnumType.STRING)
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.UNAVAILABLE;
}
