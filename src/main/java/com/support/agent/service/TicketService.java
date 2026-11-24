package com.support.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.support.agent.common.exception.BusinessException;
import com.support.agent.model.dto.approval.ApprovalView;
import com.support.agent.model.dto.chat.AgentStepView;
import com.support.agent.model.dto.chat.Citation;
import com.support.agent.model.dto.chat.MessageView;
import com.support.agent.model.dto.ticket.CreateTicketRequest;
import com.support.agent.model.dto.ticket.TicketDetailResponse;
import com.support.agent.model.dto.ticket.TicketResponse;
import com.support.agent.model.dto.ticket.UpdateTicketStatusRequest;
import com.support.agent.model.entity.Ticket;
import com.support.agent.model.entity.TicketMessage;
import com.support.agent.model.enums.Intent;
import com.support.agent.model.enums.MessageRole;
import com.support.agent.model.enums.TicketPriority;
import com.support.agent.model.enums.TicketStatus;
import com.support.agent.repository.AgentTraceRepository;
import com.support.agent.repository.ApprovalRequestRepository;
import com.support.agent.repository.TicketMessageRepository;
import com.support.agent.repository.TicketRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
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

@Service
@RequiredArgsConstructor
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    private static final EnumSet<TicketStatus> ACTIVE_STATUSES = EnumSet.of(
            TicketStatus.PENDING, TicketStatus.IN_PROGRESS, TicketStatus.WAITING_APPROVAL, TicketStatus.ESCALATED);
    private static final DateTimeFormatter TICKET_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final TypeReference<List<Citation>> CITATION_LIST = new TypeReference<>() {
    };

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final AgentTraceRepository agentTraceRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Ticket findOrCreateActiveTicket(String conversationId, String customerNo, Intent intent, String message) {
        return ticketRepository
                .findTopByConversationIdAndStatusInOrderByCreatedAtDesc(conversationId, ACTIVE_STATUSES)
                .orElseGet(() -> {
                    Ticket ticket = new Ticket();
                    ticket.setTicketNo(nextTicketNo());
                    ticket.setConversationId(conversationId);
                    ticket.setCustomerNo(customerNo);
                    ticket.setSubject(truncate(StringUtils.hasText(message) ? message : "新的客户咨询", 255));
                    ticket.setIntent(intent == null ? Intent.UNKNOWN : intent);
                    ticket.setStatus(TicketStatus.PENDING);
                    ticket.setPriority(TicketPriority.NORMAL);
                    return ticketRepository.save(ticket);
                });
    }

    @Transactional
    public Ticket createTicket(CreateTicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setTicketNo(nextTicketNo());
        ticket.setConversationId(StringUtils.hasText(request.conversationId())
                ? request.conversationId()
                : "manual-" + UUID.randomUUID());
        ticket.setCustomerNo(request.customerNo());
        ticket.setSubject(request.subject());
        ticket.setIntent(request.intent() == null ? Intent.UNKNOWN : request.intent());
        ticket.setPriority(request.priority() == null ? TicketPriority.NORMAL : request.priority());
        ticket.setStatus(TicketStatus.PENDING);
        ticket = ticketRepository.save(ticket);

        if (StringUtils.hasText(request.description())) {
            appendMessage(ticket.getId(), ticket.getConversationId(), MessageRole.USER,
                    request.description(), List.of(), null);
        }
        return ticket;
    }

    @Transactional
    public void appendMessage(UUID ticketId, String conversationId, MessageRole role, String content,
                              List<Citation> citations, String agentName) {
        TicketMessage message = new TicketMessage();
        message.setTicketId(ticketId);
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setAgentName(agentName);
        message.setCitations(writeCitations(citations));
        ticketMessageRepository.save(message);
    }

    @Transactional
    public Ticket updateStatus(String ticketNo, UpdateTicketStatusRequest request) {
        Ticket ticket = getByTicketNo(ticketNo);
        ticket.setStatus(request.status());
        if (StringUtils.hasText(request.assignee())) {
            ticket.setAssignee(request.assignee());
        }
        if (StringUtils.hasText(request.resolution())) {
            ticket.setResolution(request.resolution());
        }
        if (request.status() == TicketStatus.RESOLVED || request.status() == TicketStatus.CLOSED) {
            ticket.setClosedAt(Instant.now());
        }
        return ticketRepository.save(ticket);
    }

    @Transactional
    public void markWaitingApproval(Ticket ticket) {
        ticket.setStatus(TicketStatus.WAITING_APPROVAL);
        ticketRepository.save(ticket);
    }

    @Transactional
    public void markEscalated(Ticket ticket) {
        ticket.setStatus(TicketStatus.ESCALATED);
        ticket.setPriority(TicketPriority.HIGH);
        ticket.setAssignee("human-queue");
        ticketRepository.save(ticket);
    }

    @Transactional
    public void markInProgress(Ticket ticket) {
        if (ticket.getStatus() == TicketStatus.PENDING) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticketRepository.save(ticket);
        }
    }

    @Transactional(readOnly = true)
    public Ticket getByTicketNo(String ticketNo) {
        return ticketRepository.findByTicketNo(ticketNo)
                .orElseThrow(() -> BusinessException.notFound("工单不存在: " + ticketNo));
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> list(TicketStatus status, String customerNo, Pageable pageable) {
        Page<Ticket> page;
        if (status != null) {
            page = ticketRepository.findByStatus(status, pageable);
        } else if (StringUtils.hasText(customerNo)) {
            page = ticketRepository.findByCustomerNo(customerNo, pageable);
        } else {
            page = ticketRepository.findAll(pageable);
        }
        return page.map(TicketResponse::from);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> statusCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (TicketStatus status : TicketStatus.values()) {
            counts.put(status.name(), ticketRepository.countByStatus(status));
        }
        counts.put("TOTAL", ticketRepository.count());
        return counts;
    }

    @Transactional(readOnly = true)
    public List<MessageView> messages(String conversationId) {
        return ticketMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(this::toMessageView)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketDetailResponse detail(String ticketNo) {
        Ticket ticket = getByTicketNo(ticketNo);
        List<MessageView> messages = ticketMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId()).stream()
                .map(this::toMessageView)
                .toList();
        List<AgentStepView> traces = agentTraceRepository.findByTicketIdOrderByStepNoAsc(ticket.getId()).stream()
                .map(trace -> new AgentStepView(
                        trace.getAgent(), trace.getAction(), trace.getDecision(),
                        trace.getLatencyMs() == null ? 0L : trace.getLatencyMs()))
                .toList();
        List<ApprovalView> approvals = approvalRequestRepository.findByTicketIdOrderByCreatedAtDesc(ticket.getId())
                .stream()
                .map(ApprovalView::from)
                .toList();
        return new TicketDetailResponse(TicketResponse.from(ticket), messages, traces, approvals);
    }

    public MessageView toMessageView(TicketMessage message) {
        return new MessageView(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getAgentName(),
                readCitations(message.getCitations()),
                message.getCreatedAt());
    }

    private String writeCitations(List<Citation> citations) {
        if (citations == null || citations.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(citations);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize citations", ex);
            return "[]";
        }
    }

    private List<Citation> readCitations(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, CITATION_LIST);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to deserialize citations", ex);
            return List.of();
        }
    }

    private String nextTicketNo() {
        return "T" + LocalDate.now().format(TICKET_DATE) + "-"
                + Integer.toString(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    private String truncate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}
