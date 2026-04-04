package org.dispatchsystem.ai.dto;

import lombok.Builder;
import lombok.Data;
import org.dispatchsystem.ride.domain.RideStatus;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class AiRideExplanationResponse {
    private Long rideId;
    private RideStatus finalStatus;
    private String selectionExplanation;
    private String failureExplanation;
    private String cancellationExplanation;
    @Builder.Default
    private List<AiDriverDecisionExplanationDTO> selectedDrivers = new ArrayList<>();
    @Builder.Default
    private List<AiDriverDecisionExplanationDTO> skippedDrivers = new ArrayList<>();
}
