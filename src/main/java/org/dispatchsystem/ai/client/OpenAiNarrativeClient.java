package org.dispatchsystem.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OpenAiNarrativeClient implements AiNarrativeClient {
    private final AiClientProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenAiNarrativeClient(AiClientProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String generateRideNarrative(String prompt, String fallbackNarrative) {
        return generateNarrative(prompt, fallbackNarrative, "ride_narrative");
    }

    @Override
    public String generateOpsNarrative(String prompt, String fallbackNarrative) {
        return generateNarrative(prompt, fallbackNarrative, "ops_narrative");
    }

    private boolean isConfigured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    private String generateNarrative(String prompt, String fallbackNarrative, String schemaName) {
        if (!isConfigured()) {
            return fallbackNarrative;
        }

        try {
            JsonNode jsonNode = restClient.post()
                    .uri("/responses")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .body(buildRequestBody(prompt, schemaName))
                    .retrieve()
                    .body(JsonNode.class);

            String parsed = extractNarrative(jsonNode);
            return parsed == null || parsed.isBlank() ? fallbackNarrative : parsed;
        } catch (Exception ignored) {
            return fallbackNarrative;
        }
    }

    private Map<String, Object> buildRequestBody(String prompt, String schemaName) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("properties", Map.of(
                "narrative", Map.of("type", "string")
        ));
        schema.put("required", java.util.List.of("narrative"));

        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", schemaName);
        format.put("strict", true);
        format.put("schema", schema);

        Map<String, Object> text = new LinkedHashMap<>();
        text.put("format", format);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", properties.getModel());
        request.put("input", prompt);
        request.put("text", text);
        return request;
    }

    private String extractNarrative(JsonNode node) {
        if (node == null) {
            return null;
        }

        JsonNode outputText = node.get("output_text");
        if (outputText != null && outputText.isTextual()) {
            return extractNarrativeFromText(outputText.asText());
        }

        JsonNode output = node.get("output");
        if (output != null && output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.get("content");
                if (content != null && content.isArray()) {
                    for (JsonNode contentItem : content) {
                        JsonNode textNode = contentItem.get("text");
                        if (textNode != null && textNode.isTextual()) {
                            String extracted = extractNarrativeFromText(textNode.asText());
                            if (extracted != null && !extracted.isBlank()) {
                                return extracted;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private String extractNarrativeFromText(String text) {
        try {
            JsonNode parsed = objectMapper.readTree(text);
            JsonNode narrative = parsed.get("narrative");
            return narrative != null && narrative.isTextual() ? narrative.asText() : text;
        } catch (Exception ignored) {
            return text;
        }
    }
}
