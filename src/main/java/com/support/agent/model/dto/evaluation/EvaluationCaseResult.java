package com.support.agent.model.dto.evaluation;

import com.support.agent.model.enums.Intent;

public record EvaluationCaseResult(
        String id,
        String message,
        Intent expectedIntent,
        Intent actualIntent,
        double confidence,
        boolean intentPassed,
        String expectedTool,
        boolean toolInvoked,
        boolean expectApproval,
        boolean approvalRequested,
        boolean passed,
        String answerPreview) {
}
