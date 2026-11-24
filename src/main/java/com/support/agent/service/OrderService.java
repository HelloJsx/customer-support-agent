package com.support.agent.service;

import com.support.agent.common.exception.BusinessException;
import com.support.agent.model.dto.order.LogisticsView;
import com.support.agent.model.dto.order.OrderView;
import com.support.agent.model.entity.CustomerOrder;
import com.support.agent.model.enums.OrderStatus;
import com.support.agent.repository.CustomerOrderRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CustomerOrderRepository orderRepository;

    @Transactional(readOnly = true, noRollbackFor = BusinessException.class)
    public OrderView getOrderView(String orderNo, String customerNo) {
        return toView(requireOwnedOrder(orderNo, customerNo));
    }

    @Transactional(readOnly = true, noRollbackFor = BusinessException.class)
    public LogisticsView getLogistics(String orderNo, String customerNo) {
        CustomerOrder order = requireOwnedOrder(orderNo, customerNo);
        return new LogisticsView(
                order.getOrderNo(),
                order.getCarrier(),
                order.getTrackingNo(),
                order.getStatus().name(),
                latestEvent(order));
    }

    @Transactional(readOnly = true)
    public List<OrderView> ordersOf(String customerNo) {
        return orderRepository.findByCustomerNoOrderByCreatedAtDesc(customerNo).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional
    public void markRefunding(String orderNo) {
        orderRepository.findByOrderNo(orderNo).ifPresent(order -> {
            order.setStatus(OrderStatus.REFUNDING);
            orderRepository.save(order);
        });
    }

    @Transactional(readOnly = true)
    public CustomerOrder requireOwnedOrder(String orderNo, String customerNo) {
        CustomerOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> BusinessException.notFound("未查询到订单: " + orderNo));
        // Do not reveal that another customer's order exists.
        if (!order.getCustomerNo().equals(customerNo)) {
            throw BusinessException.notFound("未查询到订单: " + orderNo);
        }
        return order;
    }

    private OrderView toView(CustomerOrder order) {
        return new OrderView(
                order.getOrderNo(),
                order.getCustomerNo(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getItemSummary(),
                order.getTrackingNo(),
                order.getCarrier(),
                order.getPaidAt(),
                order.getDeliveredAt());
    }

    private String latestEvent(CustomerOrder order) {
        return switch (order.getStatus()) {
            case PAID -> "商家已接单，正在备货";
            case SHIPPED -> "包裹已揽收，运输中";
            case DELIVERED -> "包裹已签收";
            case CANCELLED -> "订单已取消";
            case REFUNDING -> "退款处理中";
        };
    }
}
