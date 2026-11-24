package com.support.agent.agent;

import com.support.agent.common.exception.BusinessException;
import com.support.agent.common.util.OrderNumbers;
import com.support.agent.model.dto.refund.RefundDraftView;
import com.support.agent.model.dto.refund.RefundEligibilityView;
import com.support.agent.model.enums.AgentType;
import com.support.agent.model.enums.RefundStatus;
import com.support.agent.service.RefundService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Creates refund drafts only. Execution is gated by a human approval, so the agent can never move
 * money on its own.
 */
@Component
@RequiredArgsConstructor
public class RefundAgent implements SupportAgent {

    private final RefundService refundService;

    @Override
    public AgentResponse handle(AgentContext context) {
        Optional<String> orderNo = OrderNumbers.extract(context.getUserMessage());
        if (orderNo.isEmpty()) {
            return AgentResponse.builder()
                    .agent(AgentType.REFUND)
                    .content("请提供需要退款的订单号（形如 ORD-2026001），我先为您判断退款资格。")
                    .build();
        }

        String number = orderNo.get();
        try {
            RefundEligibilityView eligibility = refundService.checkEligibility(number, context.getCustomerNo());
            if (!eligibility.eligible()) {
                return AgentResponse.builder()
                        .agent(AgentType.REFUND)
                        .content("订单 " + number + " 暂不支持退款：" + eligibility.reason()
                                + "。如仍有疑问，可回复“转人工”由人工客服进一步处理。")
                        .toolsUsed(List.of("checkRefundEligibility(" + number + ")"))
                        .build();
            }

            RefundDraftView draft = refundService.createDraft(
                    number,
                    context.getCustomerNo(),
                    "用户申请退款：" + context.getUserMessage(),
                    context.getTicketId(),
                    context.getConversationId());

            boolean pending = draft.status() == RefundStatus.PENDING_APPROVAL;
            String content = pending
                    ? "已为您创建退款草稿，退款金额 " + draft.amount() + " 元。\n"
                            + "审批单号：" + draft.approvalNo() + "\n"
                            + "退款属于敏感操作，需人工审批通过后才会执行，请耐心等待。"
                    : "该订单已有退款申请，当前状态：" + draft.status() + "。";

            return AgentResponse.builder()
                    .agent(AgentType.REFUND)
                    .content(content)
                    .requiresApproval(pending)
                    .approvalNo(draft.approvalNo())
                    .refundNo(draft.refundNo())
                    .toolsUsed(List.of(
                            "checkRefundEligibility(" + number + ")",
                            "draftRefund(" + number + ")"))
                    .build();
        } catch (BusinessException ex) {
            return AgentResponse.builder()
                    .agent(AgentType.REFUND)
                    .content(ex.getMessage() + "。请核对订单号后重试，或回复“转人工”由人工客服协助。")
                    .toolsUsed(List.of("checkRefundEligibility(" + number + ")"))
                    .build();
        }
    }
}
