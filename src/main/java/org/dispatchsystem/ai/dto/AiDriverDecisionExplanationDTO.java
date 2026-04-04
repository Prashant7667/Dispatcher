package org.dispatchsystem.ai.dto;

import lombok.Builder;
import lombok.Data;
import org.dispatchsystem.common.events.domains.EventType;
import org.dispatchsystem.common.events.domains.ReasonCode;

import java.util.List;

@Data
@Builder
public class AiDriverDecisionExplanationDTO {
    private Long driverId;
    private String driverName;
    private Integer dispatchAttempt;
    private Integer dispatchRank;
    private EventType eventType;
    private Double pickupDistanceKm;
    private Double candidateScore;
    private AiRideExplanationRequest.ScoreBreakdown scoreBreakdown;
    private String explanation;
    private List<ReasonCode> positiveReasons;
    private List<ReasonCode> negativeReasons;
}
