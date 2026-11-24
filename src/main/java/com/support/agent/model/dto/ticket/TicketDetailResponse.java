package com.support.agent.model.dto.ticket;

import com.support.agent.model.dto.approval.ApprovalView;
import com.support.agent.model.dto.chat.AgentStepView;
import com.support.agent.model.dto.chat.MessageView;
import java.util.List;

public record TicketDetailResponse(
        TicketResponse ticket,
        List<MessageView> messages,
        List<AgentStepView> traces,
        List<ApprovalView> approvals) {
}
