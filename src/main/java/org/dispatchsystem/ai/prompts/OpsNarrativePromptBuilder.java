package org.dispatchsystem.ai.prompts;

import org.dispatchsystem.ai.dto.AiOpsSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class OpsNarrativePromptBuilder {
    public String build(AiOpsSummaryResponse response) {
        StringBuilder prompt = new StringBuilder("""
                Rewrite the dispatch ops summary into a concise admin-facing summary.
                Use only the provided metrics and trends. Do not invent new facts.
                Keep it to 2-3 short sentences.
                Facts:
                """);
        prompt.append("\nAcceptance rate: ").append(response.getAcceptanceRate());
        prompt.append("\nAverage dispatch time seconds: ").append(response.getAverageDispatchTimeSeconds());
        prompt.append("\nTop cancellation reasons: ").append(response.getTopCancellationReasons());
        prompt.append("\nTop dispatch failure reasons: ").append(response.getTopDispatchFailureReasons());
        prompt.append("\nLow supply zones: ").append(response.getLowSupplyZones());
        prompt.append("\nCurrent summary: ").append(response.getNarrative());
        return prompt.toString();
    }
}
