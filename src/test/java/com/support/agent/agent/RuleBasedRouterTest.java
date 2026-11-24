package com.support.agent.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.support.agent.model.enums.Intent;
import org.junit.jupiter.api.Test;

class RuleBasedRouterTest {

    private final RuleBasedRouter router = new RuleBasedRouter();

    @Test
    void routesExplicitRefundToRefundAgent() {
        RoutingDecision decision = router.route("我要退款，订单 ORD-2026001");
        assertThat(decision.intent()).isEqualTo(Intent.REFUND);
        assertThat(decision.confidence()).isGreaterThan(0.5);
    }

    @Test
    void routesLogisticsQuestionToOrderAgent() {
        RoutingDecision decision = router.route("帮我查一下物流 ORD-2026002");
        assertThat(decision.intent()).isEqualTo(Intent.ORDER);
    }

    @Test
    void routesPolicyQuestionToKnowledgeAgent() {
        RoutingDecision decision = router.route("你们的退货政策是什么？");
        assertThat(decision.intent()).isEqualTo(Intent.KNOWLEDGE);
    }

    @Test
    void routesRefundTimingQuestionToKnowledgeAgent() {
        RoutingDecision decision = router.route("退款什么时候到账？");
        assertThat(decision.intent()).isEqualTo(Intent.KNOWLEDGE);
    }

    @Test
    void routesHumanRequestToEscalation() {
        RoutingDecision decision = router.route("转人工客服");
        assertThat(decision.intent()).isEqualTo(Intent.ESCALATION);
    }

    @Test
    void fallsBackToKnowledgeForGreeting() {
        RoutingDecision decision = router.route("你好");
        assertThat(decision.intent()).isEqualTo(Intent.KNOWLEDGE);
        assertThat(decision.confidence()).isEqualTo(0.3);
        assertThat(decision.reason()).isEqualTo("rule:fallback");
    }
}
