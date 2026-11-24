package com.support.agent.agent;

import com.support.agent.model.enums.Intent;
import dev.langchain4j.data.message.ChatMessage;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/**
 * Shared state passed through the agent pipeline. Immutable by design: each agent returns an
 * {@link AgentResponse} and the orchestrator owns persistence.
 */
@Getter
@Builder
public class AgentContext {

    private final String conversationId;
    private final String customerNo;
    private final String userMessage;
    private final Intent intent;
    private final double confidence;
    private final String routingReason;
    private final UUID ticketId;
    private final String ticketNo;
    private final List<ChatMessage> history;
}
