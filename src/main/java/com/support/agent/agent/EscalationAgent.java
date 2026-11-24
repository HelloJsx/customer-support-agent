package com.support.agent.agent;

import com.support.agent.model.entity.Ticket;
import com.support.agent.model.enums.AgentType;
import com.support.agent.service.TicketService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EscalationAgent implements SupportAgent {

    private final TicketService ticketService;

    @Override
    public AgentResponse handle(AgentContext context) {
        Ticket ticket = ticketService.getByTicketNo(context.getTicketNo());
        ticketService.markEscalated(ticket);

        String content = "已为您转接人工客服，工单号：" + ticket.getTicketNo() + "。\n"
                + "人工客服会尽快跟进，您也可以继续在此补充问题细节。";
        return AgentResponse.builder()
                .agent(AgentType.ESCALATION)
                .content(content)
                .toolsUsed(List.of("escalateTicket(" + ticket.getTicketNo() + ")"))
                .build();
    }
}
