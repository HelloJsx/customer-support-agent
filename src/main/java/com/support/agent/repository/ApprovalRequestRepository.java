package com.support.agent.repository;

import com.support.agent.model.entity.ApprovalRequest;
import com.support.agent.model.enums.ApprovalStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {

    Optional<ApprovalRequest> findByApprovalNo(String approvalNo);

    Page<ApprovalRequest> findByStatus(ApprovalStatus status, Pageable pageable);

    List<ApprovalRequest> findByTicketIdOrderByCreatedAtDesc(UUID ticketId);

    long countByStatus(ApprovalStatus status);
}
