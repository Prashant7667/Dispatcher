package org.dispatchsystem.ai.dto;

import lombok.Builder;
import lombok.Data;
import org.dispatchsystem.common.events.domains.ReasonCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class AiOpsSummaryResponse {
    private LocalDateTime from;
    private LocalDateTime to;
    private double acceptanceRate;
    private double averageDispatchTimeSeconds;
    @Builder.Default
    private List<ReasonCountDTO> topCancellationReasons = new ArrayList<>();
    @Builder.Default
    private List<ReasonCountDTO> topDispatchFailureReasons = new ArrayList<>();
    @Builder.Default
    private List<ZonePressureDTO> lowSupplyZones = new ArrayList<>();
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
