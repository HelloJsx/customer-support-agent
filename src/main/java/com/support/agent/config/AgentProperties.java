package com.support.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.agent")
public class AgentProperties {

    private boolean reviewerEnabled = true;

    private boolean seedDemoData = true;
}
