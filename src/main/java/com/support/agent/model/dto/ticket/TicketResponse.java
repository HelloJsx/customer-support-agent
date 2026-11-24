package com.support.agent.model.dto.ticket;

import com.support.agent.model.entity.Ticket;
import com.support.agent.model.enums.Intent;
import com.support.agent.model.enums.TicketPriority;
import com.support.agent.model.enums.TicketStatus;
import java.time.Instant;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        String ticketNo,
        String conversationId,
        String customerNo,
        String subject,
        Intent intent,
        TicketStatus status,
        TicketPriority priority,
        String assignee,
        String resolution,
        Instant createdAt,
        Instant updatedAt,
        Instant closedAt) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getConversationId(),
                ticket.getCustomerNo(),
                ticket.getSubject(),
                ticket.getIntent(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getAssignee(),
                ticket.getResolution(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getClosedAt());
    }
}
