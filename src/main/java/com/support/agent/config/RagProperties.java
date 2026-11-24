package com.support.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {

    /** none | pgvector */
    private String vectorStore = "none";

    private int dimension = 1536;

    private int topK = 4;

    private double minScore = 0.5;

    private String knowledgePath = "knowledge";
}
