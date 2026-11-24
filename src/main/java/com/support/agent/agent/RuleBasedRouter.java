package com.support.agent.agent;

import com.support.agent.common.util.OrderNumbers;
import com.support.agent.model.enums.Intent;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Deterministic intent router used when no LLM is configured and as a fallback when the LLM output
 * cannot be parsed. Keeping this path deterministic makes the evaluation suite reproducible.
 */
@Component
public class RuleBasedRouter {

    private static final List<String> ESCALATION_WORDS = List.of(
            "投诉", "举报", "起诉", "律师", "欺诈", "骗子", "差评", "曝光", "监管", "转人工", "人工客服", "人工");
    private static final List<String> REFUND_WORDS = List.of(
            "退款", "退货", "退钱", "售后", "换货", "维修", "补偿", "退还");
    private static final List<String> ORDER_WORDS = List.of(
            "订单", "物流", "快递", "发货", "签收", "配送", "运单", "包裹", "到货", "order", "tracking");
    private static final List<String> KNOWLEDGE_WORDS = List.of(
            "政策", "规则", "流程", "怎么", "如何", "什么", "多久", "什么时候", "多长时间", "是否",
            "支持", "保修", "发票", "会员", "权益", "价格", "支付", "地址", "电话", "密码", "账号",
            "优惠", "活动", "产品", "功能", "使用", "说明");
    private static final List<String> REFUND_ACTION_WORDS = List.of(
            "我要", "我想", "帮我", "申请", "办理", "发起", "提交", "要求", "退掉", "退一下");
    private static final List<String> HOW_TO_WORDS = List.of(
            "怎么", "如何", "政策", "规则", "流程", "什么时候", "多久", "多长时间");

    public RoutingDecision route(String message) {
        String text = message == null ? "" : message.toLowerCase(Locale.ROOT);
        boolean hasOrderNo = OrderNumbers.extract(message).isPresent();

        int escalation = matches(text, ESCALATION_WORDS);
        int refund = matches(text, REFUND_WORDS);
        int order = matches(text, ORDER_WORDS);
        int knowledge = matches(text, KNOWLEDGE_WORDS);

        if (hasOrderNo) {
            order += 2;
        }
        if (refund > 0 && matches(text, REFUND_ACTION_WORDS) > 0) {
            refund += 3;
        }
        // "怎么退款 / 退款政策 / 退款多久到账" are knowledge questions, not refund actions.
        if (refund > 0 && matches(text, HOW_TO_WORDS) > 0 && !containsAny(text, "帮我", "我要", "我想")) {
            knowledge += 3;
            refund = Math.max(0, refund - 2);
        }

        Intent intent;
        int max;
        if (escalation > 0) {
            intent = Intent.ESCALATION;
            max = escalation;
        } else if (refund > 0 && refund >= order) {
            intent = Intent.REFUND;
            max = refund;
        } else if (order > 0) {
            intent = Intent.ORDER;
            max = order;
        } else if (knowledge > 0) {
            intent = Intent.KNOWLEDGE;
            max = knowledge;
        } else {
            return new RoutingDecision(Intent.KNOWLEDGE, 0.3, "rule:fallback");
        }

        int total = escalation + refund + order + knowledge;
        double confidence = Math.min(0.95, 0.5 + (double) max / Math.max(1, total));
        String reason = String.format(
                "rule:escalation=%d,refund=%d,order=%d,knowledge=%d", escalation, refund, order, knowledge);
        return new RoutingDecision(intent, round(confidence), reason);
    }

    private int matches(String text, List<String> words) {
        int count = 0;
        for (String word : words) {
            if (text.contains(word)) {
                count++;
            }
        }
        return count;
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
