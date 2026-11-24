package com.support.agent.tool;

import com.support.agent.common.exception.BusinessException;
import com.support.agent.model.dto.refund.RefundDraftView;
import com.support.agent.model.dto.refund.RefundEligibilityView;
import com.support.agent.service.RefundService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Refund tools. {@code draftRefund} only creates a pending approval; it never executes a refund.
 */
@Component
@RequiredArgsConstructor
public class RefundTool {

    private final RefundService refundService;

    @Tool(name = "checkRefundEligibility", value = "判断订单是否满足退款条件，返回是否可退、原因和最大可退金额。")
    public RefundEligibilityView checkRefundEligibility(
            @P("订单号，例如 ORD-2026001") String orderNo,
            @P("客户编号，例如 CUST-001") String customerNo) {
        return refundService.checkEligibility(orderNo, customerNo);
    }

    @Tool(name = "draftRefund", value = "创建退款草稿并发起人工审批。该工具不会直接执行退款。")
    public RefundDraftView draftRefund(
            @P("订单号") String orderNo,
            @P("客户编号") String customerNo,
            @P("退款原因") String reason,
            @P("关联工单 ID (UUID)") String ticketId,
            @P("会话 ID") String conversationId) {
        try {
            return refundService.createDraft(
                    orderNo, customerNo, reason, UUID.fromString(ticketId), conversationId);
        } catch (IllegalArgumentException ex) {
            throw BusinessException.badRequest("ticketId 不是合法的 UUID");
        }
    }
}
