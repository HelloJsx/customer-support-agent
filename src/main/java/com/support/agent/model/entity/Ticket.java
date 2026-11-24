package com.support.agent.model.entity;

import com.support.agent.model.enums.Intent;
import com.support.agent.model.enums.TicketPriority;
import com.support.agent.model.enums.TicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tickets")
public class Ticket extends BaseEntity {

    @Column(name = "ticket_no", nullable = false, unique = true, length = 64)
    private String ticketNo;

    @Column(name = "conversation_id", nullable = false, length = 64)
    private String conversationId;

    @Column(name = "customer_no", nullable = false, length = 64)
    private String customerNo;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "intent", nullable = false, length = 32)
    private Intent intent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TicketStatus status = TicketStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 16)
    private TicketPriority priority = TicketPriority.NORMAL;

    @Column(name = "assignee", length = 64)
    private String assignee;

    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;

    @Column(name = "closed_at")
    private Instant closedAt;
}
