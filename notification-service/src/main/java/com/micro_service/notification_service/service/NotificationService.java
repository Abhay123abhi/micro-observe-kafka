package com.micro_service.notification_service.service;

import com.micro_service.notification_service.config.NotificationProperties;
import com.techie.microservices.order.event.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;
    private final NotificationProperties properties;

    public NotificationService(JavaMailSender javaMailSender, TemplateEngine templateEngine,
                               NotificationProperties properties) {
        this.javaMailSender = javaMailSender;
        this.templateEngine = templateEngine;
        this.properties = properties;
    }

    @KafkaListener(topics = "order-placed")
    public void listen(OrderPlacedEvent orderPlacedEvent) {
        log.info("Received order confirmation event for order {}", orderPlacedEvent.getOrderNumber());
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            var messageHelper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name());

            messageHelper.setFrom(properties.from());
            messageHelper.setTo(orderPlacedEvent.getEmail().toString());
            messageHelper.setSubject("Your order is confirmed | " + orderPlacedEvent.getOrderNumber());

            var context = new Context();
            context.setVariable("firstName", orderPlacedEvent.getFirstName());
            context.setVariable("lastName", orderPlacedEvent.getLastName());
            context.setVariable("orderNumber", orderPlacedEvent.getOrderNumber());
            context.setVariable("skuCode", orderPlacedEvent.getSkuCode());
            context.setVariable("quantity", orderPlacedEvent.getQuantity());
            context.setVariable("totalAmount", orderPlacedEvent.getTotalAmount());
            if (orderPlacedEvent.getPlacedAt() != null) {
                var placedAt = LocalDateTime.parse(orderPlacedEvent.getPlacedAt().toString())
                        .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
                context.setVariable("placedAt", placedAt);
            }
            context.setVariable("year", Year.now().getValue());

            messageHelper.setText(templateEngine.process("order-placed", context), true);
        };
        try {
            javaMailSender.send(messagePreparator);
            log.info("Order notification email sent for order {}", orderPlacedEvent.getOrderNumber());
        } catch (MailException e) {
            log.error("Failed to send order confirmation for order {}", orderPlacedEvent.getOrderNumber(), e);
            throw new IllegalStateException("Failed to send order notification", e);
        }
    }
}
