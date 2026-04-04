package org.dispatchsystem.ai.ops;

import org.dispatchsystem.ai.client.AiNarrativeClient;
import org.dispatchsystem.ai.dto.AiOpsSummaryRequest;
import org.dispatchsystem.ai.dto.AiOpsSummaryResponse;
import org.dispatchsystem.ai.explanations.ReasonCodeNarrativeMapper;
import org.dispatchsystem.ai.prompts.OpsNarrativePromptBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OpsInsightService {
    private final OpsMetricAssembler opsMetricAssembler;
    private final ReasonCodeNarrativeMapper reasonCodeNarrativeMapper;
    private final AiNarrativeClient aiNarrativeClient;
    private final OpsNarrativePromptBuilder opsNarrativePromptBuilder;

    public OpsInsightService(OpsMetricAssembler opsMetricAssembler,
                             ReasonCodeNarrativeMapper reasonCodeNarrativeMapper,
                             AiNarrativeClient aiNarrativeClient,
                             OpsNarrativePromptBuilder opsNarrativePromptBuilder) {
        this.opsMetricAssembler = opsMetricAssembler;
        this.reasonCodeNarrativeMapper = reasonCodeNarrativeMapper;
        this.aiNarrativeClient = aiNarrativeClient;
        this.opsNarrativePromptBuilder = opsNarrativePromptBuilder;
    }

    public AiOpsSummaryResponse getOpsSummary(LocalDateTime from, LocalDateTime to) {
        AiOpsSummaryResponse metrics = opsMetricAssembler.assemble(
                AiOpsSummaryRequest.builder().from(from).to(to).build()
        );
        String fallbackNarrative = buildFallbackNarrative(metrics);
        metrics.setNarrative(aiNarrativeClient.generateOpsNarrative(
                opsNarrativePromptBuilder.build(metrics),
                fallbackNarrative
        ));
        return metrics;
    }

    private String buildFallbackNarrative(AiOpsSummaryResponse response) {
        List<AiOpsSummaryResponse.ReasonCountDTO> topDispatchFailureReasons = response.getTopDispatchFailureReasons();
        List<AiOpsSummaryResponse.ReasonCountDTO> topCancellationReasons = response.getTopCancellationReasons();
        List<AiOpsSummaryResponse.ZonePressureDTO> lowSupplyZones = response.getLowSupplyZones();

        String topFailure = topDispatchFailureReasons.isEmpty()
                ? "No major dispatch failure reason stood out."
                : "Top dispatch issue was " + phrase(topDispatchFailureReasons.get(0).getReasonCode()) + ".";
        String topCancellation = topCancellationReasons.isEmpty()
                ? "No cancellation pattern stood out."
                : "Top cancellation reason was " + phrase(topCancellationReasons.get(0).getReasonCode()) + ".";
        String lowSupply = lowSupplyZones.isEmpty()
                ? "No low-supply hotspot was detected."
                : "Lowest supply pressure appeared around " + lowSupplyZones.get(0).getZoneLabel() + ".";
        return "Acceptance rate was " + response.getAcceptanceRate() + "%. " + topFailure + " " + topCancellation + " " + lowSupply;
    }

    private String phrase(org.dispatchsystem.common.events.domains.ReasonCode reasonCode) {
        return reasonCodeNarrativeMapper.summarizeNegativeReasons(List.of(reasonCode), reasonCode.name());
    }
}
