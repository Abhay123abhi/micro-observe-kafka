package com.micro_service.order.service.service;

import com.micro_service.order.service.api.OrderRequest;
import com.micro_service.order.service.api.OrderResponse;
import com.micro_service.order.service.client.InventoryClient;
import com.micro_service.order.service.model.Order;
import com.micro_service.order.service.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    public OrderService(OrderRepository orderRepository, InventoryClient inventoryClient) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
    }

    public OrderResponse placeOrder(OrderRequest orderRequest) {
        boolean inStock = inventoryClient.checkStock(orderRequest.skuCode(), orderRequest.quantity()).inStock();
        if (inStock) {
            var order = new Order(UUID.randomUUID().toString(), orderRequest.skuCode(),
                    orderRequest.price().multiply(BigDecimal.valueOf(orderRequest.quantity())), orderRequest.quantity());
            orderRepository.save(order);

            log.info("Recorded workload transaction {} for SKU {}", order.orderNumber(), order.skuCode());
            return new OrderResponse(order.orderNumber());
        }

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Product with SKU " + orderRequest.skuCode() + " is out of stock");
    }
}
