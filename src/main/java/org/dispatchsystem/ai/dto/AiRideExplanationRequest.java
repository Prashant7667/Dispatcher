package org.dispatchsystem.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.dispatchsystem.common.events.domains.EventType;
import org.dispatchsystem.common.events.domains.ReasonCode;
import org.dispatchsystem.driver.domain.VehicleClass;
import org.dispatchsystem.ride.domain.BookingType;
import org.dispatchsystem.ride.domain.RideStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AiRideExplanationRequest {
    private Long rideId;
    private RideStatus finalStatus;
    private RideFacts ride;
    private List<CandidateFacts> candidates;
    private OutcomeFacts outcome;

    @Data
    @AllArgsConstructor
    @Builder
    public static class RideFacts {
        private BookingType bookingType;
        private VehicleClass requestedVehicleClass;
        private Integer requiredLuggageCapacity;
        private Integer estimatedDurationMinutes;
        private LocalDateTime scheduledStart;
        private double pickupLatitude;
        private double pickupLongitude;
        private LocalDateTime dispatchStartedAt;
        private LocalDateTime driverAssignedAt;
        private LocalDateTime cancelledAt;
    }

    @Data
    @Builder
    public static class CandidateFacts {
        private Long driverId;
        private String driverName;
        private Integer dispatchAttempt;
        private Integer dispatchRank;
        private EventType eventType;
        private Boolean eligible;
        private Double pickupDistanceKm;
        private Double score;
        private ScoreBreakdown scoreBreakdown;
        private List<ReasonCode> acceptedReasons;
        private List<ReasonCode> rejectedReasons;
    }

    @Data
    @Builder
    public static class ScoreBreakdown {
        private Double constraintsScore;
        private Double distanceScore;
        private Double ratingScore;
    }

    @Data
    @Builder
    public static class OutcomeFacts {
        private EventType finalEventType;
        private Long assignedDriverId;
        private String assignedDriverName;
        private List<ReasonCode> finalReasonCodes;
        private String finalReasonDetails;
    }
}
