package org.dispatchsystem.ai.prompts;

import org.dispatchsystem.ai.dto.AiDriverDecisionExplanationDTO;
import org.dispatchsystem.ai.dto.AiRideExplanationResponse;
import org.springframework.stereotype.Component;

@Component
public class RideNarrativePromptBuilder {
    public String build(AiRideExplanationResponse response) {
        StringBuilder prompt = new StringBuilder("""
                Rewrite the dispatch explanation into a concise, admin-friendly sentence.
                Use only the provided facts. Do not invent new reasons or metrics.
                Keep it to 1-2 sentences.
                Facts:
                """);

        if (response.getSelectedDrivers() != null && !response.getSelectedDrivers().isEmpty()) {
            AiDriverDecisionExplanationDTO selected = response.getSelectedDrivers().get(response.getSelectedDrivers().size() - 1);
            prompt.append("\nSelected driver: ").append(selected.getDriverName());
            if (selected.getDispatchRank() != null) {
                prompt.append("\nRank: ").append(selected.getDispatchRank());
            }
            if (selected.getPickupDistanceKm() != null) {
                prompt.append("\nPickup distance km: ").append(selected.getPickupDistanceKm());
            }
            if (selected.getCandidateScore() != null) {
                prompt.append("\nCandidate score: ").append(selected.getCandidateScore());
            }
            if (selected.getScoreBreakdown() != null) {
                prompt.append("\nConstraint score: ").append(selected.getScoreBreakdown().getConstraintsScore());
                prompt.append("\nDistance score: ").append(selected.getScoreBreakdown().getDistanceScore());
                prompt.append("\nRating score: ").append(selected.getScoreBreakdown().getRatingScore());
            }
        }

        prompt.append("\nCurrent explanation: ").append(response.getSelectionExplanation());
        if (response.getSkippedDrivers() != null && !response.getSkippedDrivers().isEmpty()) {
            prompt.append("\nSkipped driver summary:");
            response.getSkippedDrivers().forEach(driver -> prompt.append("\n- ").append(driver.getExplanation()));
        }
        if (response.getFailureExplanation() != null) {
            prompt.append("\nFailure explanation: ").append(response.getFailureExplanation());
        }
        if (response.getCancellationExplanation() != null) {
            prompt.append("\nCancellation explanation: ").append(response.getCancellationExplanation());
        }
        return prompt.toString();
    }
}
