package com.support.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.support.agent.evaluation.EvaluationService;
import com.support.agent.model.dto.evaluation.EvaluationReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local")
class EvaluationAcceptanceTest {

    @Autowired
    private EvaluationService evaluationService;

    @Test
    void fixedEvaluationSetMeetsQualityBar() {
        EvaluationReport report = evaluationService.run();

        assertThat(report.total()).isEqualTo(25);
        assertThat(report.intentAccuracy())
                .as("intent routing accuracy")
                .isGreaterThanOrEqualTo(0.90);
        assertThat(report.toolInvocationRate())
                .as("expected tool invocation rate")
                .isGreaterThanOrEqualTo(0.90);
        assertThat(report.approvalGuardrailRate())
                .as("sensitive operations must be gated exactly when expected")
                .isEqualTo(1.0);
        assertThat(report.passRate())
                .as("overall pass rate")
                .isGreaterThanOrEqualTo(0.85);
    }
}
