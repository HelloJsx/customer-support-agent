package com.support.agent.model.dto.chat;

import com.support.agent.model.enums.Intent;
import java.util.List;

public record ChatResponse(
        String conversationId,
        String ticketNo,
        Intent intent,
        double confidence,
        String routingReason,
        String answer,
        List<Citation> citations,
        boolean requiresApproval,
        String approvalNo,
        String refundNo,
        List<String> toolsUsed,
        List<AgentStepView> trace) {
}
