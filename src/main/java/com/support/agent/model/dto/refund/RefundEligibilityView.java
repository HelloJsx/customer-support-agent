package com.support.agent.model.dto.refund;

import java.math.BigDecimal;

public record RefundEligibilityView(boolean eligible, String reason, BigDecimal maxAmount) {
}
