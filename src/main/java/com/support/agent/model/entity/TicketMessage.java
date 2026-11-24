package com.support.agent.model.entity;

import com.support.agent.model.enums.MessageRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ticket_messages")
public class TicketMessage extends BaseEntity {

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "conversation_id", nullable = false, length = 64)
    private String conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_role", nullable = false, length = 32)
    private MessageRole role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /** JSON array of citations, kept as text for portability between H2 and PostgreSQL. */
    @Column(name = "citations", columnDefinition = "TEXT")
    private String citations;

    @Column(name = "agent_name", length = 64)
    private String agentName;
}
