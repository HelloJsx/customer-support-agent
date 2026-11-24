package com.support.agent.tool;

import com.support.agent.model.dto.order.LogisticsView;
import com.support.agent.model.dto.order.OrderView;
import com.support.agent.service.OrderService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Business tools exposed to LangChain4j AiServices. {@code customerNo} is an explicit parameter so
 * that data isolation is enforced at the tool boundary.
 */
@Component
@RequiredArgsConstructor
public class OrderTool {

    private final OrderService orderService;

    @Tool(name = "queryOrder", value = "根据订单号查询订单状态、商品、金额。订单号形如 ORD-2026001。")
    public OrderView queryOrder(
            @P("订单号，例如 ORD-2026001") String orderNo,
            @P("客户编号，例如 CUST-001") String customerNo) {
        return orderService.getOrderView(orderNo, customerNo);
    }

    @Tool(name = "queryLogistics", value = "根据订单号查询承运商、运单号和最新物流进度。")
    public LogisticsView queryLogistics(
            @P("订单号，例如 ORD-2026001") String orderNo,
            @P("客户编号，例如 CUST-001") String customerNo) {
        return orderService.getLogistics(orderNo, customerNo);
    }

    @Tool(name = "listCustomerOrders", value = "列出某个客户最近的订单。")
    public List<OrderView> listCustomerOrders(@P("客户编号，例如 CUST-001") String customerNo) {
        return orderService.ordersOf(customerNo);
    }
}
