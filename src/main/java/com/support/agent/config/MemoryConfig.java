package com.support.agent.config;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemoryConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.memory", name = "store", havingValue = "in-memory", matchIfMissing = true)
    public ChatMemoryStore inMemoryChatMemoryStore() {
        return new InMemoryChatMemoryStore();
    }

    @Bean
    public dev.langchain4j.memory.chat.ChatMemoryProvider chatMemoryProvider(
            ChatMemoryStore chatMemoryStore, MemoryProperties properties) {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(properties.getMaxMessages())
                .chatMemoryStore(chatMemoryStore)
                .build();
    }
}
