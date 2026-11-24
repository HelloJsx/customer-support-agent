package com.support.agent.model.entity;

import com.support.agent.model.enums.RefundStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "refund_requests")
public class RefundRequest extends BaseEntity {

    @Column(name = "refund_no", nullable = false, unique = true, length = 64)
    private String refundNo;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "order_no", nullable = false, length = 64)
    private String orderNo;

    @Column(name = "customer_no", nullable = false, length = 64)
    private String customerNo;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private RefundStatus status = RefundStatus.DRAFT;

    @Column(name = "approval_id")
    private UUID approvalId;
}
