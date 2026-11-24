package com.support.agent.model.dto.chat;

import com.support.agent.model.enums.MessageRole;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageView(
        UUID id,
        MessageRole role,
        String content,
        String agentName,
        List<Citation> citations,
        Instant createdAt) {
}
