package com.support.agent.service;

import com.support.agent.model.dto.chat.AgentStepView;
import com.support.agent.model.entity.AgentTrace;
import com.support.agent.model.enums.AgentType;
import com.support.agent.repository.AgentTraceRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AgentTraceService {

    private final AgentTraceRepository agentTraceRepository;

    @Transactional
    public AgentStepView record(UUID ticketId, String conversationId, int stepNo, AgentType agent,
                                String action, String inputSummary, String outputSummary,
                                String decision, long latencyMs) {
        AgentTrace trace = new AgentTrace();
        trace.setTicketId(ticketId);
        trace.setConversationId(conversationId);
        trace.setStepNo(stepNo);
        trace.setAgent(agent);
        trace.setAction(action);
        trace.setInputSummary(inputSummary);
        trace.setOutputSummary(outputSummary);
        trace.setDecision(decision);
        trace.setLatencyMs(latencyMs);
        agentTraceRepository.save(trace);
        return new AgentStepView(agent, action, decision, latencyMs);
    }

    @Transactional(readOnly = true)
    public List<AgentStepView> byTicket(UUID ticketId) {
        return agentTraceRepository.findByTicketIdOrderByStepNoAsc(ticketId).stream()
                .map(trace -> new AgentStepView(
                        trace.getAgent(), trace.getAction(), trace.getDecision(),
                        trace.getLatencyMs() == null ? 0L : trace.getLatencyMs()))
                .toList();
    }
}
