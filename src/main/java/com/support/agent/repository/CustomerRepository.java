package com.support.agent.repository;

import com.support.agent.model.entity.Customer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByCustomerNo(String customerNo);

    boolean existsByCustomerNo(String customerNo);
}
