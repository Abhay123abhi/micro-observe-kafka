package com.micro_service.order.service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;
    private String skuCode;
    private BigDecimal price;
    private Integer quantity;

    private LocalDateTime orderDate;
    @PrePersist
    public void prePersist() {
        this.orderDate = LocalDateTime.now();
    }
}
