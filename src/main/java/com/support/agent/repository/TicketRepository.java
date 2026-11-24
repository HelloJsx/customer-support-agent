package com.support.agent.repository;

import com.support.agent.model.entity.Ticket;
import com.support.agent.model.enums.TicketStatus;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Optional<Ticket> findByTicketNo(String ticketNo);

    Optional<Ticket> findTopByConversationIdAndStatusInOrderByCreatedAtDesc(
            String conversationId, Collection<TicketStatus> statuses);

    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    Page<Ticket> findByCustomerNo(String customerNo, Pageable pageable);

    long countByStatus(TicketStatus status);
}
