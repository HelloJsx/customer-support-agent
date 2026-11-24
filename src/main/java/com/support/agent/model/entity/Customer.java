package com.support.agent.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

    @Column(name = "customer_no", nullable = false, unique = true, length = 64)
    private String customerNo;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 32)
    private String phone;
}
