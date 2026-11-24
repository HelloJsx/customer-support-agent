package com.support.agent.model.entity;

import com.support.agent.model.enums.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class CustomerOrder extends BaseEntity {

    @Column(name = "order_no", nullable = false, unique = true, length = 64)
    private String orderNo;

    @Column(name = "customer_no", nullable = false, length = 64)
    private String customerNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 8)
    private String currency = "CNY";

    @Column(name = "item_summary", columnDefinition = "TEXT")
    private String itemSummary;

    @Column(name = "tracking_no", length = 64)
    private String trackingNo;

    @Column(name = "carrier", length = 64)
    private String carrier;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;
}
