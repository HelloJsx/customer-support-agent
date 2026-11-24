package com.support.agent.controller;

import com.support.agent.common.api.ApiResponse;
import com.support.agent.evaluation.EvaluationService;
import com.support.agent.model.dto.evaluation.EvaluationReport;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Evaluation", description = "固定评估集回归")
@RestController
@RequestMapping("/api/v1/evaluation")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PostMapping("/run")
    public ApiResponse<EvaluationReport> run() {
        return ApiResponse.ok(evaluationService.run());
    }
}
