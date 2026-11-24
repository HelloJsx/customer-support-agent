package com.support.agent.model.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "customerNo 不能为空")
        @Size(max = 64, message = "customerNo 长度不能超过 64")
        String customerNo,

        @Size(max = 64, message = "conversationId 长度不能超过 64")
        String conversationId,

        @NotBlank(message = "message 不能为空")
        @Size(max = 2000, message = "message 长度不能超过 2000")
        String message) {
}
