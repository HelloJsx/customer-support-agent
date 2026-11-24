package com.support.agent.model.dto.ticket;

import com.support.agent.model.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTicketStatusRequest(
        @NotNull(message = "status 不能为空") TicketStatus status,

        @Size(max = 2000) String resolution,

        @Size(max = 64) String assignee) {
}
