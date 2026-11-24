package com.support.agent.config;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed conversation memory. Redis outages degrade to stateless chat instead of failing
 * the request; the durable transcript is still persisted in {@code ticket_messages}.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.memory", name = "store", havingValue = "redis")
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final Logger log = LoggerFactory.getLogger(RedisChatMemoryStore.class);
    private static final String KEY_PREFIX = "chat:memory:";

    private final StringRedisTemplate redisTemplate;
    private final MemoryProperties properties;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        try {
            String json = redisTemplate.opsForValue().get(key(memoryId));
            return Optional.ofNullable(json)
                    .map(ChatMessageDeserializer::messagesFromJson)
                    .orElseGet(List::of);
        } catch (Exception ex) {
            log.warn("Failed to read chat memory {}: {}", memoryId, ex.getMessage());
            return List.of();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        try {
            redisTemplate.opsForValue().set(
                    key(memoryId), ChatMessageSerializer.messagesToJson(messages), properties.getTtl());
        } catch (Exception ex) {
            log.warn("Failed to persist chat memory {}: {}", memoryId, ex.getMessage());
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        try {
            redisTemplate.delete(key(memoryId));
        } catch (Exception ex) {
            log.warn("Failed to delete chat memory {}: {}", memoryId, ex.getMessage());
        }
    }

    private String key(Object memoryId) {
        return KEY_PREFIX + memoryId;
    }
}
