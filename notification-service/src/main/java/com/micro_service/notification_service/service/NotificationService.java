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
    public void listen(OrderPlacedEvent orderPlacedEvent){
        log.info("Got Message from order-placed topic {}", orderPlacedEvent);
        MimeMessagePreparator messagePreparator = mimeMessage -> {

            MimeMessageHelper messageHelper =
                    new MimeMessageHelper(mimeMessage, true, "UTF-8");

            messageHelper.setFrom(properties.from());
            messageHelper.setTo(orderPlacedEvent.getEmail().toString());
            messageHelper.setSubject(
                    "🎉 Order Confirmed | " + orderPlacedEvent.getOrderNumber()
            );

            // Prepare template variables
            Context context = new Context();
            context.setVariable("firstName", orderPlacedEvent.getFirstName());
            context.setVariable("lastName", orderPlacedEvent.getLastName());
            context.setVariable("orderNumber", orderPlacedEvent.getOrderNumber());

            // Load HTML template
            String htmlContent =
                    templateEngine.process("order-placed", context);

            messageHelper.setText(htmlContent, true);
        };
        try {
            javaMailSender.send(messagePreparator);
            log.info("Order notification email sent for order {}", orderPlacedEvent.getOrderNumber());
        } catch (MailException e) {
            log.error("Exception occurred when sending mail", e);
            throw new IllegalStateException("Failed to send order notification", e);
        }
    }
}
