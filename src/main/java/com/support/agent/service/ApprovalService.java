package com.support.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.support.agent.common.exception.BusinessException;
import com.support.agent.model.dto.approval.ApprovalDecisionRequest;
import com.support.agent.model.dto.approval.ApprovalView;
import com.support.agent.model.entity.ApprovalRequest;
import com.support.agent.model.entity.RefundRequest;
import com.support.agent.model.entity.Ticket;
import com.support.agent.model.entity.TicketMessage;
import com.support.agent.model.enums.ApprovalActionType;
import com.support.agent.model.enums.ApprovalStatus;
import com.support.agent.model.enums.MessageRole;
import com.support.agent.model.enums.RefundStatus;
import com.support.agent.model.enums.TicketStatus;
import com.support.agent.repository.ApprovalRequestRepository;
import com.support.agent.repository.CustomerOrderRepository;
import com.support.agent.repository.RefundRequestRepository;
import com.support.agent.repository.TicketMessageRepository;
import com.support.agent.repository.TicketRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Human-in-the-loop approval workflow. Sensitive effects are only applied after an explicit
 * approval decision.
 */
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);
    private static final DateTimeFormatter APPROVAL_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ApprovalRequestRepository approvalRepository;
    private final RefundRequestRepository refundRepository;
    private final CustomerOrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ApprovalRequest request(ApprovalActionType actionType, UUID ticketId, String conversationId,
                                   String summary, Map<String, Object> payload, String requestedBy) {
        ApprovalRequest request = new ApprovalRequest();
        request.setApprovalNo(nextApprovalNo());
        request.setTicketId(ticketId);
        request.setConversationId(conversationId);
        request.setActionType(actionType);
        request.setSummary(summary);
        request.setPayload(writePayload(payload));
        request.setStatus(ApprovalStatus.PENDING);
        request.setRequestedBy(requestedBy);
        return approvalRepository.save(request);
    }

    @Transactional
    public ApprovalView decide(String approvalNo, ApprovalDecisionRequest decision) {
        ApprovalRequest request = approvalRepository.findByApprovalNo(approvalNo)
                .orElseThrow(() -> BusinessException.notFound("审批单不存在: " + approvalNo));
        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw BusinessException.invalidState("审批单已处理: " + approvalNo);
        }

        request.setStatus(Boolean.TRUE.equals(decision.approved())
                ? ApprovalStatus.APPROVED
                : ApprovalStatus.REJECTED);
        request.setDecidedBy(decision.decidedBy());
        request.setDecidedAt(Instant.now());
        request.setComment(decision.comment());
        approvalRepository.save(request);

        String effect = Boolean.TRUE.equals(decision.approved()) ? "APPROVED" : "REJECTED";
        if (request.getActionType() == ApprovalActionType.REFUND) {
            applyRefundDecision(request, Boolean.TRUE.equals(decision.approved()));
        } else {
            // Extension point: plug in order-update / data-deletion handlers here.
            log.info("Approval {} for action {} decided as {}", approvalNo, request.getActionType(), effect);
        }

        updateTicket(request, Boolean.TRUE.equals(decision.approved()), decision);
        return ApprovalView.from(request);
    }

    @Transactional(readOnly = true)
    public ApprovalView get(String approvalNo) {
        return approvalRepository.findByApprovalNo(approvalNo)
                .map(ApprovalView::from)
                .orElseThrow(() -> BusinessException.notFound("审批单不存在: " + approvalNo));
    }

    @Transactional(readOnly = true)
    public Page<ApprovalView> list(ApprovalStatus status, Pageable pageable) {
        Page<ApprovalRequest> page = status == null
                ? approvalRepository.findAll(pageable)
                : approvalRepository.findByStatus(status, pageable);
        return page.map(ApprovalView::from);
    }

    @Transactional(readOnly = true)
    public ApprovalRequest getEntity(String approvalNo) {
        return approvalRepository.findByApprovalNo(approvalNo)
                .orElseThrow(() -> BusinessException.notFound("审批单不存在: " + approvalNo));
    }

    @Transactional(readOnly = true)
    public ApprovalRequest getById(UUID id) {
        return approvalRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("审批单不存在: " + id));
    }

    private void applyRefundDecision(ApprovalRequest request, boolean approved) {
        RefundRequest refund = refundRepository.findByApprovalId(request.getId())
                .orElseThrow(() -> BusinessException.notFound("退款申请不存在"));
        refund.setStatus(approved ? RefundStatus.APPROVED : RefundStatus.REJECTED);
        refundRepository.save(refund);
        if (approved) {
            orderService.markRefunding(refund.getOrderNo());
        }
    }

    private void updateTicket(ApprovalRequest request, boolean approved, ApprovalDecisionRequest decision) {
        ticketRepository.findById(request.getTicketId()).ifPresent(ticket -> {
            ticket.setStatus(approved ? TicketStatus.RESOLVED : TicketStatus.IN_PROGRESS);
            if (approved) {
                ticket.setClosedAt(Instant.now());
                ticket.setResolution("人工审批通过：" + request.getSummary());
            } else {
                ticket.setResolution("人工审批驳回：" + (decision.comment() == null ? "" : decision.comment()));
            }
            ticketRepository.save(ticket);
            appendSystemMessage(ticket, approved, request, decision);
        });
    }

    private void appendSystemMessage(Ticket ticket, boolean approved, ApprovalRequest request,
                                     ApprovalDecisionRequest decision) {
        TicketMessage message = new TicketMessage();
        message.setTicketId(ticket.getId());
        message.setConversationId(ticket.getConversationId());
        message.setRole(MessageRole.SYSTEM);
        message.setAgentName("approval-service");
        message.setCitations("[]");
        message.setContent(approved
                ? "人工审批通过：" + request.getSummary() + "，操作已执行。"
                : "人工审批驳回：" + request.getSummary()
                        + (StringUtils.hasText(decision.comment()) ? "，原因：" + decision.comment() : ""));
        ticketMessageRepository.save(message);
    }

    private String writePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize approval payload", ex);
            return "{}";
        }
    }

    private String nextApprovalNo() {
        return "AP" + LocalDate.now().format(APPROVAL_DATE) + "-"
                + Integer.toString(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }
}
