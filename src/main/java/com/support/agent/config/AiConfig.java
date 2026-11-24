package com.support.agent.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * AI wiring. Beans are only created when explicitly enabled so the application can boot and be
 * evaluated in deterministic mode without any API key.
 */
@Configuration
@EnableConfigurationProperties({AiProperties.class, RagProperties.class, MemoryProperties.class, AgentProperties.class})
public class AiConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "true")
    public ChatModel chatModel(AiProperties properties) {
        return OpenAiChatModel.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .modelName(properties.getChatModel())
                .temperature(properties.getTemperature())
                .timeout(properties.getTimeout())
                .maxRetries(properties.getMaxRetries())
                .logRequests(properties.isLogRequests())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "true")
    public EmbeddingModel embeddingModel(AiProperties properties) {
        String baseUrl = StringUtils.hasText(properties.getEmbeddingBaseUrl())
                ? properties.getEmbeddingBaseUrl()
                : properties.getBaseUrl();
        String apiKey = StringUtils.hasText(properties.getEmbeddingApiKey())
                ? properties.getEmbeddingApiKey()
                : properties.getApiKey();
        return OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(properties.getEmbeddingModel())
                .timeout(properties.getTimeout())
                .maxRetries(properties.getMaxRetries())
                .logRequests(properties.isLogRequests())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.rag", name = "vector-store", havingValue = "pgvector")
    public EmbeddingStore<TextSegment> pgVectorEmbeddingStore(DataSource dataSource, RagProperties properties) {
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table("knowledge_embedding")
                .dimension(properties.getDimension())
                .createTable(true)
                .build();
    }
}
