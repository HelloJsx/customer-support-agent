package com.support.agent.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Short-term conversation memory. Backed by Redis in distributed deployments and by an in-memory
 * store for local development. The durable transcript lives in the ticket_messages table.
 */
@Service
@RequiredArgsConstructor
public class ConversationMemoryService {

    private final ChatMemoryProvider chatMemoryProvider;

    public List<ChatMessage> history(String conversationId) {
        return List.copyOf(memory(conversationId).messages());
    }

    public void addUserMessage(String conversationId, String content) {
        memory(conversationId).add(UserMessage.from(content));
    }

    public void addAssistantMessage(String conversationId, String content) {
        memory(conversationId).add(AiMessage.from(content));
    }

    public void clear(String conversationId) {
        memory(conversationId).clear();
    }

    private ChatMemory memory(String conversationId) {
        return chatMemoryProvider.get(conversationId);
    }
}
