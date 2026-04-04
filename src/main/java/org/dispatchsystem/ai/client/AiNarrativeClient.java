package org.dispatchsystem.ai.client;

public interface AiNarrativeClient {
    String generateRideNarrative(String prompt, String fallbackNarrative);
    String generateOpsNarrative(String prompt, String fallbackNarrative);
}
