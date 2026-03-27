package org.dispatchsystem.ride.dto;

import lombok.Data;
import org.dispatchsystem.common.events.domains.EventType;
import org.dispatchsystem.common.events.domains.ReasonCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DispatchTimelineEventDTO {
    private Long eventId;
    private Long rideId;
    private Long driverId;
    private String driverName;
    private EventType eventType;
    private Integer dispatchAttempt;
    private String reasonDetails;
    private LocalDateTime createdAt;
    private List<ReasonCode> positiveReasons;
    private List<ReasonCode> negativeReasons;
}
