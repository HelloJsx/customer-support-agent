package com.support.agent.model.dto.refund;

import com.support.agent.model.enums.RefundStatus;
import java.math.BigDecimal;

public record RefundDraftView(
        String refundNo,
        String approvalNo,
        String orderNo,
        BigDecimal amount,
        RefundStatus status,
        String message) {
}
