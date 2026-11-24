package com.support.agent.model.dto.ticket;

import com.support.agent.model.enums.Intent;
import com.support.agent.model.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank(message = "customerNo 不能为空")
        @Size(max = 64) String customerNo,

        @Size(max = 64) String conversationId,

        @NotBlank(message = "subject 不能为空")
        @Size(max = 255) String subject,

        @Size(max = 4000) String description,

        Intent intent,

        TicketPriority priority) {
}
