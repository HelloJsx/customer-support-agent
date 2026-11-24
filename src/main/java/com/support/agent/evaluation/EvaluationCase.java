package com.support.agent.evaluation;

import com.support.agent.model.enums.Intent;

public record EvaluationCase(
        String id,
        String message,
        Intent expectedIntent,
        String expectedTool,
        boolean expectApproval) {
}
