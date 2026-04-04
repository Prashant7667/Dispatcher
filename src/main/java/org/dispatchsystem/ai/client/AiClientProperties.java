package org.dispatchsystem.ai.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "dispatch.ai")
public class AiClientProperties {
    private String apiKey;
    private String model = "gpt-5-mini";
    private String baseUrl = "https://api.openai.com/v1";
}
