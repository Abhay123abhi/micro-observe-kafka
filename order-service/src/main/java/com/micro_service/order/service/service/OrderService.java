package com.micro_service.order.service.service;

import com.micro_service.order.service.api.OrderRequest;
import com.micro_service.order.service.api.OrderResponse;
import com.micro_service.order.service.client.InventoryClient;
import com.micro_service.order.service.model.Order;
import com.micro_service.order.service.repository.OrderRepository;
import com.techie.microservices.order.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public OrderResponse placeOrder(OrderRequest orderRequest) {
        boolean inStock = inventoryClient.checkStock(orderRequest.skuCode(), orderRequest.quantity()).inStock();
        if (inStock) {
            Order order = new Order();
            order.setOrderNumber(UUID.randomUUID().toString());
            order.setPrice(orderRequest.price().multiply(BigDecimal.valueOf(orderRequest.quantity())));
            order.setQuantity(orderRequest.quantity());
            order.setSkuCode(orderRequest.skuCode());
            orderRepository.save(order);

            var user = orderRequest.userDetails();
            var orderPlacedEvent = OrderPlacedEvent.newBuilder()
                    .setOrderNumber(order.getOrderNumber())
                    .setEmail(user.email())
                    .setFirstName(user.firstName())
                    .setLastName(user.lastName())
                    .setSkuCode(order.getSkuCode())
                    .setQuantity(order.getQuantity())
                    .setTotalAmount(order.getPrice().toPlainString())
                    .setPlacedAt(order.getOrderDate().toString())
                    .build();
            log.info("Publishing order confirmation event for order {}", order.getOrderNumber());
            kafkaTemplate.sendDefault(orderPlacedEvent);
            return new OrderResponse(order.getOrderNumber());
        }

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Product with SKU " + orderRequest.skuCode() + " is out of stock");
    }
}
