package com.support.agent.model.dto.approval;

import com.support.agent.model.entity.ApprovalRequest;
import com.support.agent.model.enums.ApprovalActionType;
import com.support.agent.model.enums.ApprovalStatus;
import java.time.Instant;
import java.util.UUID;

public record ApprovalView(
        UUID id,
        String approvalNo,
        UUID ticketId,
        String conversationId,
        ApprovalActionType actionType,
        String summary,
        String payload,
        ApprovalStatus status,
        String requestedBy,
        Instant requestedAt,
        String decidedBy,
        Instant decidedAt,
        String comment) {

    public static ApprovalView from(ApprovalRequest request) {
        return new ApprovalView(
                request.getId(),
                request.getApprovalNo(),
                request.getTicketId(),
                request.getConversationId(),
                request.getActionType(),
                request.getSummary(),
                request.getPayload(),
                request.getStatus(),
                request.getRequestedBy(),
                request.getCreatedAt(),
                request.getDecidedBy(),
                request.getDecidedAt(),
                request.getComment());
    }
}
