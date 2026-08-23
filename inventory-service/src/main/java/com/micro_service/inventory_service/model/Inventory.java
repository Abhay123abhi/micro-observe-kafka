package com.micro_service.inventory_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "t_inventory")
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 100)
    private String skuCode;

    @Column(nullable = false)
    private Integer quantity;

    protected Inventory() {
    }

    public Inventory(String skuCode, int quantity) {
        this.skuCode = skuCode;
        this.quantity = quantity;
    }
}
