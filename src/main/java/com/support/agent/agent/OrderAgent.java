package com.support.agent.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.support.agent.common.exception.BusinessException;
import com.support.agent.common.util.OrderNumbers;
import com.support.agent.config.PromptLoader;
import com.support.agent.model.dto.order.LogisticsView;
import com.support.agent.model.dto.order.OrderView;
import com.support.agent.model.enums.AgentType;
import com.support.agent.model.enums.OrderStatus;
import com.support.agent.service.LlmClient;
import com.support.agent.service.OrderService;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class OrderAgent implements SupportAgent {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final OrderService orderService;
    private final LlmClient llmClient;
    private final PromptLoader promptLoader;
    private final ObjectMapper objectMapper;

    @Override
    public AgentResponse handle(AgentContext context) {
        Optional<String> orderNo = OrderNumbers.extract(context.getUserMessage());
        if (orderNo.isEmpty()) {
            return AgentResponse.builder()
                    .agent(AgentType.ORDER)
                    .content("请提供您的订单号（形如 ORD-2026001），我帮您查询订单和物流信息。")
                    .build();
        }

        String number = orderNo.get();
        try {
            OrderView order = orderService.getOrderView(number, context.getCustomerNo());
            LogisticsView logistics = orderService.getLogistics(number, context.getCustomerNo());
            String fallback = composeAnswer(order, logistics);
            String answer = fallback;

            if (llmClient.available()) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("order", order);
                data.put("logistics", logistics);
                try {
                    String systemPrompt = promptLoader.get("order")
                            + "\n\n业务数据(JSON)：\n" + objectMapper.writeValueAsString(data);
                    answer = llmClient.complete(systemPrompt, context.getHistory())
                            .filter(StringUtils::hasText)
                            .orElse(fallback);
                } catch (JsonProcessingException ex) {
                    answer = fallback;
                }
            }

            return AgentResponse.builder()
                    .agent(AgentType.ORDER)
                    .content(answer)
                    .toolsUsed(List.of("queryOrder(" + number + ")", "queryLogistics(" + number + ")"))
                    .build();
        } catch (BusinessException ex) {
            return AgentResponse.builder()
                    .agent(AgentType.ORDER)
                    .content(ex.getMessage() + "。请核对订单号后重试，或回复“转人工”由人工客服协助。")
                    .toolsUsed(List.of("queryOrder(" + number + ")"))
                    .build();
        }
    }

    private String composeAnswer(OrderView order, LogisticsView logistics) {
        StringBuilder answer = new StringBuilder();
        answer.append("订单 ").append(order.orderNo()).append(" 当前状态：")
                .append(statusLabel(order.status())).append("\n")
                .append("商品：").append(StringUtils.hasText(order.itemSummary()) ? order.itemSummary() : "暂无")
                .append("\n")
                .append("金额：").append(order.currency()).append(" ").append(order.totalAmount()).append("\n");
        if (StringUtils.hasText(order.trackingNo())) {
            answer.append("物流：").append(order.carrier()).append("，运单号 ")
                    .append(order.trackingNo()).append("\n");
        } else {
            answer.append("物流：暂无运单信息\n");
        }
        answer.append("最新进度：").append(logistics.latestEvent());
        if (order.deliveredAt() != null) {
            answer.append("（签收时间：").append(TIME_FORMAT.format(order.deliveredAt())).append("）");
        }
        return answer.toString();
    }

    private String statusLabel(OrderStatus status) {
        return switch (status) {
            case PAID -> "已支付，待发货";
            case SHIPPED -> "已发货，运输中";
            case DELIVERED -> "已签收";
            case CANCELLED -> "已取消";
            case REFUNDING -> "退款处理中";
        };
    }
}
