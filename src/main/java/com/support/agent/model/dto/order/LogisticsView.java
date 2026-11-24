package com.support.agent.model.dto.order;

public record LogisticsView(
        String orderNo,
        String carrier,
        String trackingNo,
        String status,
        String latestEvent) {
}
