package com.support.agent.agent;

import com.support.agent.model.enums.Intent;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Deterministic guardrail layer applied to every agent answer before it reaches the user.
 */
@Component
@RequiredArgsConstructor
public class ReviewerAgent {

    public AgentResponse review(AgentContext context, AgentResponse draft) {
        String content = draft.content();
        if (!StringUtils.hasText(content)) {
            content = "抱歉，暂时无法生成回复，请补充更多信息或回复“转人工”由人工客服协助。";
        }

        if (context.getIntent() == Intent.REFUND && !draft.requiresApproval()
                && claimsRefundExecuted(content)) {
            content = "退款申请尚未执行。退款属于敏感操作，必须经过人工审批后才能处理。";
        }

        if (context.getIntent() == Intent.KNOWLEDGE && draft.citations().isEmpty()
                && !content.contains("转人工")) {
            content = content + "\n\n（知识库暂无直接匹配的资料，建议转人工客服确认。）";
        }

        return AgentResponse.builder()
                .agent(draft.agent())
                .content(content)
                .citations(draft.citations())
                .requiresApproval(draft.requiresApproval())
                .approvalNo(draft.approvalNo())
                .refundNo(draft.refundNo())
                .toolsUsed(draft.toolsUsed())
                .build();
    }

    private boolean claimsRefundExecuted(String content) {
        String text = content.toLowerCase(Locale.ROOT);
        return text.contains("已退款") || text.contains("退款成功") || text.contains("已完成退款");
    }
}
