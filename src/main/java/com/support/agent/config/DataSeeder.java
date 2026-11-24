package com.support.agent.config;

import com.support.agent.model.entity.Customer;
import com.support.agent.model.entity.CustomerOrder;
import com.support.agent.model.enums.OrderStatus;
import com.support.agent.repository.CustomerOrderRepository;
import com.support.agent.repository.CustomerRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a small demo dataset so the API and evaluation suite work out of the box.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.agent", name = "seed-demo-data", havingValue = "true",
        matchIfMissing = true)
public class DataSeeder implements ApplicationRunner {

    private final CustomerRepository customerRepository;
    private final CustomerOrderRepository orderRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!customerRepository.existsByCustomerNo("CUST-001")) {
            customerRepository.save(customer("CUST-001", "张晓明", "zhang@example.com", "13800000001"));
        }
        if (!customerRepository.existsByCustomerNo("CUST-002")) {
            customerRepository.save(customer("CUST-002", "李思远", "li@example.com", "13800000002"));
        }

        if (orderRepository.count() == 0) {
            Instant now = Instant.now();
            orderRepository.saveAll(List.of(
                    order("ORD-2026001", "CUST-001", OrderStatus.DELIVERED, "399.00",
                            "Nimbus 智能音箱 x1", "SF1234567890", "顺丰速运",
                            now.minus(Duration.ofDays(5)), now.minus(Duration.ofDays(2))),
                    order("ORD-2026002", "CUST-001", OrderStatus.SHIPPED, "268.00",
                            "Nimbus 降噪耳机 x1", "YT9988776655", "圆通速递",
                            now.minus(Duration.ofDays(3)), null),
                    order("ORD-2026003", "CUST-001", OrderStatus.PAID, "129.00",
                            "Nimbus 便携充电宝 x1", null, null,
                            now.minus(Duration.ofDays(1)), null),
                    order("ORD-2026004", "CUST-001", OrderStatus.CANCELLED, "899.00",
                            "Nimbus 智能手表 x1", null, null,
                            now.minus(Duration.ofDays(20)), null),
                    order("ORD-2026005", "CUST-001", OrderStatus.DELIVERED, "59.00",
                            "Nimbus 快充数据线 x2", "SF0000111122", "顺丰速运",
                            now.minus(Duration.ofDays(35)), now.minus(Duration.ofDays(30))),
                    order("ORD-2026010", "CUST-002", OrderStatus.DELIVERED, "1099.00",
                            "Nimbus 智能手表 Pro x1", "JD5566778899", "京东物流",
                            now.minus(Duration.ofDays(6)), now.minus(Duration.ofDays(1)))));
        }
    }

    private Customer customer(String customerNo, String name, String email, String phone) {
        Customer customer = new Customer();
        customer.setCustomerNo(customerNo);
        customer.setName(name);
        customer.setEmail(email);
        customer.setPhone(phone);
        return customer;
    }

    private CustomerOrder order(String orderNo, String customerNo, OrderStatus status, String amount,
                                String items, String trackingNo, String carrier,
                                Instant paidAt, Instant deliveredAt) {
        CustomerOrder order = new CustomerOrder();
        order.setOrderNo(orderNo);
        order.setCustomerNo(customerNo);
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal(amount));
        order.setCurrency("CNY");
        order.setItemSummary(items);
        order.setTrackingNo(trackingNo);
        order.setCarrier(carrier);
        order.setPaidAt(paidAt);
        order.setDeliveredAt(deliveredAt);
        return order;
    }
}
