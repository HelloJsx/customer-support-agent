package com.support.agent.service;

import com.support.agent.common.exception.BusinessException;
import com.support.agent.model.dto.refund.RefundDraftView;
import com.support.agent.model.dto.refund.RefundEligibilityView;
import com.support.agent.model.entity.ApprovalRequest;
import com.support.agent.model.entity.CustomerOrder;
import com.support.agent.model.entity.RefundRequest;
import com.support.agent.model.entity.Ticket;
import com.support.agent.model.enums.ApprovalActionType;
import com.support.agent.model.enums.OrderStatus;
import com.support.agent.model.enums.RefundStatus;
import com.support.agent.model.enums.TicketStatus;
import com.support.agent.repository.CustomerOrderRepository;
import com.support.agent.repository.RefundRequestRepository;
import com.support.agent.repository.TicketRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refund business rules. The agent can only create a draft; executing the refund requires a human
 * approval through {@link ApprovalService}.
 */
@Service
@RequiredArgsConstructor
public class RefundService {

    private static final Duration RETURN_WINDOW = Duration.ofDays(7);
    private static final DateTimeFormatter REFUND_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final EnumSet<RefundStatus> OPEN_STATUSES =
            EnumSet.of(RefundStatus.PENDING_APPROVAL, RefundStatus.APPROVED, RefundStatus.EXECUTED);

    private final CustomerOrderRepository orderRepository;
    private final RefundRequestRepository refundRepository;
    private final TicketRepository ticketRepository;
    private final ApprovalService approvalService;

    @Transactional(readOnly = true, noRollbackFor = BusinessException.class)
    public RefundEligibilityView checkEligibility(String orderNo, String customerNo) {
        CustomerOrder order = orderRepository.findByOrderNo(orderNo)
                .filter(candidate -> candidate.getCustomerNo().equals(customerNo))
                .orElseThrow(() -> BusinessException.notFound("未查询到订单: " + orderNo));
        return evaluate(order);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public RefundDraftView createDraft(String orderNo, String customerNo, String reason,
                                       UUID ticketId, String conversationId) {
        CustomerOrder order = orderRepository.findByOrderNo(orderNo)
                .filter(candidate -> candidate.getCustomerNo().equals(customerNo))
                .orElseThrow(() -> BusinessException.notFound("未查询到订单: " + orderNo));

        RefundEligibilityView eligibility = evaluate(order);
        if (!eligibility.eligible()) {
            throw BusinessException.invalidState("该订单暂不支持退款：" + eligibility.reason());
        }

        RefundRequest existing = refundRepository
                .findFirstByTicketIdAndOrderNoAndStatusInOrderByCreatedAtDesc(ticketId, orderNo, OPEN_STATUSES)
                .orElse(null);
        if (existing != null) {
            ApprovalRequest approval = existing.getApprovalId() == null
                    ? null
                    : approvalService.getById(existing.getApprovalId());
            return new RefundDraftView(
                    existing.getRefundNo(),
                    approval == null ? null : approval.getApprovalNo(),
                    existing.getOrderNo(),
                    existing.getAmount(),
                    existing.getStatus(),
                    "该订单已有退款申请，无需重复提交。");
        }

        RefundRequest refund = new RefundRequest();
        refund.setRefundNo(nextRefundNo());
        refund.setTicketId(ticketId);
        refund.setOrderNo(orderNo);
        refund.setCustomerNo(customerNo);
        refund.setAmount(order.getTotalAmount());
        refund.setReason(reason);
        refund.setStatus(RefundStatus.PENDING_APPROVAL);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("refundNo", refund.getRefundNo());
        payload.put("orderNo", orderNo);
        payload.put("customerNo", customerNo);
        payload.put("amount", order.getTotalAmount());
        payload.put("currency", order.getCurrency());
        payload.put("reason", reason);
        payload.put("eligibility", eligibility.reason());

        ApprovalRequest approval = approvalService.request(
                ApprovalActionType.REFUND,
                ticketId,
                conversationId,
                "退款审批：" + orderNo + " / ¥" + order.getTotalAmount(),
                payload,
                "refund-agent");

        refund.setApprovalId(approval.getId());
        refund = refundRepository.save(refund);

        ticketRepository.findById(ticketId).ifPresent(ticket -> {
            ticket.setStatus(TicketStatus.WAITING_APPROVAL);
            ticketRepository.save(ticket);
        });

        return new RefundDraftView(
                refund.getRefundNo(),
                approval.getApprovalNo(),
                refund.getOrderNo(),
                refund.getAmount(),
                refund.getStatus(),
                "退款草稿已生成，等待人工审批后执行。");
    }

    @Transactional(readOnly = true)
    public RefundRequest getByNo(String refundNo) {
        return refundRepository.findByRefundNo(refundNo)
                .orElseThrow(() -> BusinessException.notFound("退款申请不存在: " + refundNo));
    }

    private RefundEligibilityView evaluate(CustomerOrder order) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return new RefundEligibilityView(false, "订单已取消，无需退款", BigDecimal.ZERO);
        }
        if (order.getStatus() == OrderStatus.REFUNDING) {
            return new RefundEligibilityView(false, "该订单正在退款处理中", BigDecimal.ZERO);
        }
        if (order.getStatus() == OrderStatus.DELIVERED) {
            Instant deliveredAt = order.getDeliveredAt();
            if (deliveredAt != null && deliveredAt.isBefore(Instant.now().minus(RETURN_WINDOW))) {
                return new RefundEligibilityView(false, "已超过签收后 7 天无理由退货期限",
                        BigDecimal.ZERO);
            }
            return new RefundEligibilityView(true, "已签收且在 7 天无理由退货期内", order.getTotalAmount());
        }
        return new RefundEligibilityView(true, "订单尚未完成，可申请取消/退款", order.getTotalAmount());
    }

    private String nextRefundNo() {
        return "R" + LocalDate.now().format(REFUND_DATE) + "-"
                + Integer.toString(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }
}
