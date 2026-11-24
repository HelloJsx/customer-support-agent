package com.support.agent.model.entity;

import com.support.agent.model.enums.AgentType;
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
@Table(name = "agent_traces")
public class AgentTrace extends BaseEntity {

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "conversation_id", nullable = false, length = 64)
    private String conversationId;

    @Column(name = "step_no", nullable = false)
    private int stepNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent", nullable = false, length = 32)
    private AgentType agent;

    @Column(name = "action", nullable = false, length = 128)
    private String action;

    @Column(name = "input_summary", columnDefinition = "TEXT")
    private String inputSummary;

    @Column(name = "output_summary", columnDefinition = "TEXT")
    private String outputSummary;

    @Column(name = "decision", length = 64)
    private String decision;

    @Column(name = "latency_ms")
    private Long latencyMs;
}
