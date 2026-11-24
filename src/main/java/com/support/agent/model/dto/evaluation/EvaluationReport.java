package com.support.agent.model.dto.evaluation;

import java.time.Instant;
import java.util.List;

public record EvaluationReport(
        Instant runAt,
        int total,
        double intentAccuracy,
        double toolInvocationRate,
        double approvalGuardrailRate,
        double passRate,
        List<EvaluationCaseResult> cases) {
}
