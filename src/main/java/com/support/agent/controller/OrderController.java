package com.support.agent.controller;

import com.support.agent.common.api.ApiResponse;
import com.support.agent.model.dto.order.LogisticsView;
import com.support.agent.model.dto.order.OrderView;
import com.support.agent.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Orders", description = "订单与物流查询（演示业务数据）")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/orders/{orderNo}")
    public ApiResponse<OrderView> getOrder(
            @PathVariable String orderNo,
            @RequestParam String customerNo) {
        return ApiResponse.ok(orderService.getOrderView(orderNo, customerNo));
    }

    @GetMapping("/orders/{orderNo}/logistics")
    public ApiResponse<LogisticsView> getLogistics(
            @PathVariable String orderNo,
            @RequestParam String customerNo) {
        return ApiResponse.ok(orderService.getLogistics(orderNo, customerNo));
    }

    @GetMapping("/customers/{customerNo}/orders")
    public ApiResponse<List<OrderView>> customerOrders(@PathVariable String customerNo) {
        return ApiResponse.ok(orderService.ordersOf(customerNo));
    }
}
