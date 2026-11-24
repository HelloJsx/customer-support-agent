package com.support.agent.model.dto.approval;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApprovalDecisionRequest(
        @NotNull(message = "approved 不能为空") Boolean approved,

        @NotBlank(message = "decidedBy 不能为空")
        @Size(max = 64) String decidedBy,

        @Size(max = 500) String comment) {
}
