package com.micro_service.order.service.service;

import com.micro_service.order.service.client.InventoryClient;
import com.micro_service.order.service.dto.OrderRequest;
import com.micro_service.order.service.model.Order;
import com.micro_service.order.service.repository.OrderRepository;
import com.techie.microservices.order.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public void placeOrder(OrderRequest orderRequest) {
        boolean inStock = inventoryClient.isInStock(orderRequest.skuCode(), orderRequest.quantity());
        if(inStock){
            Order order = new Order();
            order.setOrderNumber(UUID.randomUUID().toString());
            order.setPrice(orderRequest.price().multiply(BigDecimal.valueOf(orderRequest.quantity())));
            order.setQuantity(orderRequest.quantity());
            order.setSkuCode(orderRequest.skuCode());
            orderRepository.save(order);

//             send message to kafka topic
            var orderPlacedEvent = new OrderPlacedEvent(order.getOrderNumber(), orderRequest.userDetails()
                    .email(),
                                                        orderRequest.userDetails()
                                                                .firstName(),
                                                        orderRequest.userDetails()
                                                                .lastName());
            log.info("Start- Sending OrderPlacedEvent {} to Kafka Topic", orderPlacedEvent);
            kafkaTemplate.send("order-placed", orderPlacedEvent);
            log.info("End- Sending OrderPlacedEvent {} to Kafka Topic", orderPlacedEvent);

        }else{
            throw new RuntimeException("Product with Skucode " + orderRequest.skuCode() + "is out of stock");
        }

    }
}
