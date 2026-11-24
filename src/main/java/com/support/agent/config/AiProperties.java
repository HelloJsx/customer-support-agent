package com.support.agent.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /** When false the system runs in deterministic mode (rule-based router + template answers). */
    private boolean enabled = false;

    /** OpenAI-compatible base URL, e.g. https://api.deepseek.com/v1. */
    private String baseUrl = "https://api.deepseek.com/v1";

    private String apiKey = "";

    private String chatModel = "deepseek-chat";

    private String embeddingModel = "text-embedding-3-small";

    /** Optional separate OpenAI-compatible endpoint for embeddings (e.g. when chat uses DeepSeek). */
    private String embeddingBaseUrl = "";

    private String embeddingApiKey = "";

    private double temperature = 0.2;

    private Duration timeout = Duration.ofSeconds(60);

    private int maxRetries = 2;

    private boolean logRequests = false;
}
