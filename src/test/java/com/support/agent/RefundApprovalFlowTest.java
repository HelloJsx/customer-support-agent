package com.support.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.support.agent.agent.AgentOrchestrator;
import com.support.agent.model.dto.approval.ApprovalDecisionRequest;
import com.support.agent.model.dto.chat.ChatRequest;
import com.support.agent.model.dto.chat.ChatResponse;
import com.support.agent.model.entity.CustomerOrder;
import com.support.agent.model.entity.RefundRequest;
import com.support.agent.model.entity.Ticket;
import com.support.agent.model.enums.ApprovalStatus;
import com.support.agent.model.enums.Intent;
import com.support.agent.model.enums.OrderStatus;
import com.support.agent.model.enums.RefundStatus;
import com.support.agent.model.enums.TicketStatus;
import com.support.agent.repository.CustomerOrderRepository;
import com.support.agent.repository.RefundRequestRepository;
import com.support.agent.service.ApprovalService;
import com.support.agent.service.TicketService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local")
class RefundApprovalFlowTest {

    @Autowired
    private AgentOrchestrator orchestrator;

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private RefundRequestRepository refundRepository;

    @Autowired
    private CustomerOrderRepository orderRepository;

    @Autowired
    private TicketService ticketService;

    @Test
    void refundRequiresHumanApprovalBeforeExecution() {
        ChatResponse response = orchestrator.chat(new ChatRequest(
                "CUST-001",
                "test-refund-" + UUID.randomUUID(),
                "我要退款，订单 ORD-2026003"));

        assertThat(response.intent()).isEqualTo(Intent.REFUND);
        assertThat(response.requiresApproval()).isTrue();
        assertThat(response.approvalNo()).isNotBlank();
        assertThat(response.refundNo()).isNotBlank();
        assertThat(response.toolsUsed()).anyMatch(tool -> tool.startsWith("draftRefund"));

        RefundRequest refund = refundRepository.findByRefundNo(response.refundNo()).orElseThrow();
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.PENDING_APPROVAL);
        assertThat(approvalService.get(response.approvalNo()).status()).isEqualTo(ApprovalStatus.PENDING);

        var decided = approvalService.decide(response.approvalNo(),
                new ApprovalDecisionRequest(true, "test-admin", "符合退款条件"));
        assertThat(decided.status()).isEqualTo(ApprovalStatus.APPROVED);

        RefundRequest approvedRefund = refundRepository.findByRefundNo(response.refundNo()).orElseThrow();
        assertThat(approvedRefund.getStatus()).isEqualTo(RefundStatus.APPROVED);

        CustomerOrder order = orderRepository.findByOrderNo("ORD-2026003").orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDING);

        Ticket ticket = ticketService.getByTicketNo(response.ticketNo());
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
    }
}
