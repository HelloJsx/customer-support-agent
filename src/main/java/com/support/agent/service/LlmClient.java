package com.support.agent.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Thin wrapper around LangChain4j that degrades gracefully when no model is configured or the
 * provider is temporarily unavailable.
 */
@Service
@RequiredArgsConstructor
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    private final ObjectProvider<ChatModel> chatModelProvider;

    public boolean available() {
        return chatModelProvider.getIfAvailable() != null;
    }

    public Optional<String> complete(String systemPrompt, String userMessage) {
        return complete(systemPrompt, List.of(UserMessage.from(userMessage)));
    }

    public Optional<String> complete(String systemPrompt, List<ChatMessage> messages) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            return Optional.empty();
        }

        List<ChatMessage> payload = new ArrayList<>();
        if (StringUtils.hasText(systemPrompt)) {
            payload.add(SystemMessage.from(systemPrompt));
        }
        if (messages != null) {
            payload.addAll(messages);
        }
        if (payload.isEmpty()) {
            return Optional.empty();
        }

        try {
            ChatResponse response = chatModel.chat(payload);
            AiMessage aiMessage = response.aiMessage();
            if (aiMessage == null || !StringUtils.hasText(aiMessage.text())) {
                return Optional.empty();
            }
            return Optional.of(aiMessage.text().trim());
        } catch (Exception ex) {
            log.warn("LLM call failed, falling back to deterministic answer: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
