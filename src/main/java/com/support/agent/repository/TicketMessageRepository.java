package com.support.agent.repository;

import com.support.agent.model.entity.TicketMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, UUID> {

    List<TicketMessage> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);

    List<TicketMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);
}
