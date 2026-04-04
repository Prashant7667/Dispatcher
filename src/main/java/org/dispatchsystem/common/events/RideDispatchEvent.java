package org.dispatchsystem.common.events;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dispatchsystem.common.events.domains.EventType;
import org.dispatchsystem.common.events.domains.ReasonCode;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.ride.domain.Ride;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ride_dispatch_events")
public class RideDispatchEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Ride ride;

    @ManyToOne
    private Driver driver;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    private Integer dispatchAttempt;

    private String reasonDetails;

    private LocalDateTime createdAt;

    @ElementCollection(targetClass = ReasonCode.class)
    @CollectionTable(name = "ride_dispatch_event_positive_reasons", joinColumns = @JoinColumn(name = "dispatch_event_id"))
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private List<ReasonCode> positiveReasons = new ArrayList<>();

    @ElementCollection(targetClass = ReasonCode.class)
    @CollectionTable(name = "ride_dispatch_event_negative_reasons", joinColumns = @JoinColumn(name = "dispatch_event_id"))
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private List<ReasonCode> negativeReasons = new ArrayList<>();
}
