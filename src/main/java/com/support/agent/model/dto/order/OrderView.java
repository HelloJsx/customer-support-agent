package com.support.agent.model.dto.order;

import com.support.agent.model.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderView(
        String orderNo,
        String customerNo,
        OrderStatus status,
        BigDecimal totalAmount,
        String currency,
        String itemSummary,
        String trackingNo,
        String carrier,
        Instant paidAt,
        Instant deliveredAt) {
}
