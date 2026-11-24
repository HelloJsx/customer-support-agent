package com.support.agent.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.memory")
public class MemoryProperties {

    /** in-memory | redis */
    private String store = "in-memory";

    private int maxMessages = 20;

    private Duration ttl = Duration.ofHours(24);
}
