package com.support.agent.repository;

import com.support.agent.model.entity.CustomerOrder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {

    Optional<CustomerOrder> findByOrderNo(String orderNo);

    List<CustomerOrder> findByCustomerNoOrderByCreatedAtDesc(String customerNo);
}
