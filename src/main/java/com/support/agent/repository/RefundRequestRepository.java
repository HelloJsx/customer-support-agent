package com.support.agent.repository;

import com.support.agent.model.entity.RefundRequest;
import com.support.agent.model.enums.RefundStatus;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, UUID> {

    Optional<RefundRequest> findByRefundNo(String refundNo);

    Optional<RefundRequest> findByApprovalId(UUID approvalId);

    Optional<RefundRequest> findFirstByTicketIdAndOrderNoAndStatusInOrderByCreatedAtDesc(
            UUID ticketId, String orderNo, Collection<RefundStatus> statuses);
}
