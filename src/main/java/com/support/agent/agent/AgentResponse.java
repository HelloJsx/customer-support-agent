package com.support.agent.agent;

import com.support.agent.model.dto.chat.Citation;
import com.support.agent.model.enums.AgentType;
import java.util.List;
import lombok.Builder;

@Builder
public record AgentResponse(
        AgentType agent,
        String content,
        List<Citation> citations,
        boolean requiresApproval,
        String approvalNo,
        String refundNo,
        List<String> toolsUsed) {

    public AgentResponse {
        citations = citations == null ? List.of() : List.copyOf(citations);
        toolsUsed = toolsUsed == null ? List.of() : List.copyOf(toolsUsed);
    }
}
