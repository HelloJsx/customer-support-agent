package com.support.agent.agent;

import com.support.agent.config.AgentProperties;
import com.support.agent.model.dto.chat.AgentStepView;
import com.support.agent.model.dto.chat.ChatRequest;
import com.support.agent.model.dto.chat.ChatResponse;
import com.support.agent.model.entity.Conversation;
import com.support.agent.model.entity.Ticket;
import com.support.agent.model.enums.AgentType;
import com.support.agent.model.enums.Intent;
import com.support.agent.model.enums.MessageRole;
import com.support.agent.model.enums.TicketStatus;
import com.support.agent.service.AgentTraceService;
import com.support.agent.service.ConversationMemoryService;
import com.support.agent.service.ConversationService;
import com.support.agent.service.TicketService;
import dev.langchain4j.data.message.ChatMessage;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Supervisor pipeline:
 *
 * <pre>
 * route intent -> create/attach ticket -> specialist agent -> reviewer -> persist -> respond
 * </pre>
 *
 * Every step is persisted in {@code agent_traces} for auditability.
 */
@Service
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final IntentClassifier intentClassifier;
    private final ConversationService conversationService;
    private final ConversationMemoryService conversationMemoryService;
    private final TicketService ticketService;
    private final AgentTraceService agentTraceService;
    private final KnowledgeAgent knowledgeAgent;
    private final OrderAgent orderAgent;
    private final RefundAgent refundAgent;
    private final EscalationAgent escalationAgent;
    private final ReviewerAgent reviewerAgent;
    private final AgentProperties agentProperties;

    @Transactional
    public ChatResponse chat(ChatRequest request) {
        Conversation conversation = conversationService.getOrCreate(request.conversationId(), request.customerNo());
        String conversationId = conversation.getId();
        conversationMemoryService.addUserMessage(conversationId, request.message());
        List<ChatMessage> history = conversationMemoryService.history(conversationId);

        long routeStarted = System.currentTimeMillis();
        RoutingDecision routing = intentClassifier.classify(request.message());
        long routeLatency = System.currentTimeMillis() - routeStarted;

        Ticket ticket = ticketService.findOrCreateActiveTicket(
                conversationId, request.customerNo(), routing.intent(), request.message());
        ticketService.appendMessage(
                ticket.getId(), conversationId, MessageRole.USER, request.message(), List.of(), null);

        List<AgentStepView> steps = new ArrayList<>();
        steps.add(agentTraceService.record(
                ticket.getId(), conversationId, 1, AgentType.SUPERVISOR, "classify_intent",
                request.message(), routing.intent().name(), routing.intent().name(), routeLatency));

        AgentContext context = AgentContext.builder()
                .conversationId(conversationId)
                .customerNo(request.customerNo())
                .userMessage(request.message())
                .intent(routing.intent())
                .confidence(routing.confidence())
                .routingReason(routing.reason())
                .ticketId(ticket.getId())
                .ticketNo(ticket.getTicketNo())
                .history(history)
                .build();

        long agentStarted = System.currentTimeMillis();
        AgentResponse draft = dispatch(routing.intent(), context);
        long agentLatency = System.currentTimeMillis() - agentStarted;

        int stepNo = 2;
        for (String tool : draft.toolsUsed()) {
            steps.add(agentTraceService.record(
                    ticket.getId(), conversationId, stepNo++, draft.agent(), "tool_call",
                    request.message(), tool, tool, 0));
        }
        steps.add(agentTraceService.record(
                ticket.getId(), conversationId, stepNo++, draft.agent(), "draft_answer",
                request.message(), draft.content(),
                draft.requiresApproval() ? "approval_required" : "answered", agentLatency));

        AgentResponse reviewed = draft;
        if (agentProperties.isReviewerEnabled()) {
            long reviewStarted = System.currentTimeMillis();
            reviewed = reviewerAgent.review(context, draft);
            long reviewLatency = System.currentTimeMillis() - reviewStarted;
            steps.add(agentTraceService.record(
                    ticket.getId(), conversationId, stepNo, AgentType.REVIEWER, "review_answer",
                    draft.content(), reviewed.content(),
                    reviewed.requiresApproval() ? "approval_required" : "approved", reviewLatency));
        }

        ticketService.appendMessage(
                ticket.getId(), conversationId, MessageRole.ASSISTANT,
                reviewed.content(), reviewed.citations(), reviewed.agent().name());
        conversationMemoryService.addAssistantMessage(conversationId, reviewed.content());
        conversationService.touch(conversationId, request.message());
        updateTicketStatus(ticket, routing.intent(), reviewed);

        return new ChatResponse(
                conversationId,
                ticket.getTicketNo(),
                routing.intent(),
                routing.confidence(),
                routing.reason(),
                reviewed.content(),
                reviewed.citations(),
                reviewed.requiresApproval(),
                reviewed.approvalNo(),
                reviewed.refundNo(),
                reviewed.toolsUsed(),
                steps);
    }

    private AgentResponse dispatch(Intent intent, AgentContext context) {
        return switch (intent) {
            case KNOWLEDGE -> knowledgeAgent.handle(context);
            case ORDER -> orderAgent.handle(context);
            case REFUND -> refundAgent.handle(context);
            case ESCALATION, UNKNOWN -> escalationAgent.handle(context);
        };
    }

    private void updateTicketStatus(Ticket ticket, Intent intent, AgentResponse response) {
        if (response.requiresApproval()) {
            if (ticket.getStatus() != TicketStatus.WAITING_APPROVAL) {
                ticketService.markWaitingApproval(ticket);
            }
            return;
        }
        if (intent == Intent.ESCALATION || intent == Intent.UNKNOWN) {
            return;
        }
        ticketService.markInProgress(ticket);
    }
}
