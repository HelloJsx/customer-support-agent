package com.support.agent.model.entity;

import com.support.agent.model.enums.ApprovalActionType;
import com.support.agent.model.enums.ApprovalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "approval_requests")
public class ApprovalRequest extends BaseEntity {

    @Column(name = "approval_no", nullable = false, unique = true, length = 64)
    private String approvalNo;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "conversation_id", nullable = false, length = 64)
    private String conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 32)
    private ApprovalActionType actionType;

    @Column(name = "summary", nullable = false, length = 512)
    private String summary;

    /** JSON payload with everything the approver needs to make a decision. */
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "requested_by", nullable = false, length = 64)
    private String requestedBy;

    @Column(name = "decided_by", length = 64)
    private String decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decision_comment", columnDefinition = "TEXT")
    private String comment;
}
