package org.dispatchsystem.ride.dto;

import lombok.Builder;
import lombok.Data;
import org.dispatchsystem.common.events.domains.ReasonCode;
import org.dispatchsystem.ride.domain.RideStatus;

import java.util.List;

@Data
@Builder
public class DispatchRideExplanationDTO {
    private Long rideId;
    private RideStatus finalStatus;
    private String selectionExplanation;
    private String failureExplanation;
    private String cancellationExplanation;
    private List<DriverDecisionDTO> selectedDrivers;
    private List<DriverDecisionDTO> skippedDrivers;

    @Data
    @Builder
    public static class DriverDecisionDTO {
        private Long driverId;
        private String driverName;
        private Integer dispatchAttempt;
        private String explanation;
        private List<ReasonCode> positiveReasons;
        private List<ReasonCode> negativeReasons;
    }
}
