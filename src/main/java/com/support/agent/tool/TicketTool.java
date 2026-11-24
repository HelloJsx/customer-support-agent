package com.support.agent.tool;

import com.support.agent.model.dto.ticket.CreateTicketRequest;
import com.support.agent.model.dto.ticket.TicketResponse;
import com.support.agent.model.entity.Ticket;
import com.support.agent.model.enums.Intent;
import com.support.agent.service.TicketService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketTool {

    private final TicketService ticketService;

    @Tool(name = "createTicket", value = "创建人工工单，用于升级、投诉或无法自动处理的问题。")
    public TicketResponse createTicket(
            @P("客户编号，例如 CUST-001") String customerNo,
            @P("工单标题") String subject,
            @P("问题描述") String description,
            @P("会话 ID，可为空") String conversationId) {
        Ticket ticket = ticketService.createTicket(new CreateTicketRequest(
                customerNo,
                conversationId,
                subject,
                description,
                Intent.ESCALATION,
                null));
        return TicketResponse.from(ticket);
    }
}
