package org.dispatchsystem.ride.domain;
import jakarta.persistence.*;
import lombok.Data;
import org.dispatchsystem.driver.domain.Driver;

import java.time.LocalDateTime;
@Data
@Entity
@Table(name = "ride_offers")
public class RideOffer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private Ride ride;
    @ManyToOne
    private Driver driver;
    @Enumerated(EnumType.STRING)
    private OfferStatus status;
    private LocalDateTime sentAt;
    private LocalDateTime expiresAt;
    private LocalDateTime respondedAt;

}
