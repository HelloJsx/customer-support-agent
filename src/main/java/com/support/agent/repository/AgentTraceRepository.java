package com.support.agent.repository;

import com.support.agent.model.entity.AgentTrace;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentTraceRepository extends JpaRepository<AgentTrace, UUID> {

    List<AgentTrace> findByTicketIdOrderByStepNoAsc(UUID ticketId);

    List<AgentTrace> findByConversationIdOrderByStepNoAsc(String conversationId);
}
