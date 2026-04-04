package org.dispatchsystem.ride.dto;

import lombok.Builder;
import lombok.Data;
import org.dispatchsystem.common.events.domains.ReasonCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DispatchOpsSummaryDTO {
    private LocalDateTime from;
    private LocalDateTime to;
    private double acceptanceRate;
    private double averageDispatchTimeSeconds;
    private List<ReasonCountDTO> topCancellationReasons;
    private List<ReasonCountDTO> topDispatchFailureReasons;
    private List<ZonePressureDTO> lowSupplyZones;
    private String narrative;

    @Data
    @Builder
    public static class ReasonCountDTO {
        private ReasonCode reasonCode;
        private long count;
    }

    @Data
    @Builder
    public static class ZonePressureDTO {
        private String zoneLabel;
        private long noSupplyCount;
    }
}
