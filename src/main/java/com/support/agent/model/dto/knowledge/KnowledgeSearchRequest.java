package com.support.agent.model.dto.knowledge;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record KnowledgeSearchRequest(
        @NotBlank(message = "query 不能为空") String query,

        @Min(1) @Max(10) Integer topK) {
}
