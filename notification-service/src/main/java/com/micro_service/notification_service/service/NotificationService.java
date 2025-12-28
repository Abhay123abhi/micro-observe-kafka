package com.micro_service.notification_service.service;

import com.techie.microservices.order.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @KafkaListener(topics = "order-placed")
    public void listen(OrderPlacedEvent orderPlacedEvent){
        log.info("Got Message from order-placed topic {}", orderPlacedEvent);
        MimeMessagePreparator messagePreparator = mimeMessage -> {

            MimeMessageHelper messageHelper =
                    new MimeMessageHelper(mimeMessage, true, "UTF-8");

            messageHelper.setFrom("accessdenied801@gmail.com");
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
        };        try {
            javaMailSender.send(messagePreparator);
            log.info("Order Notifcation email sent to email ID!!");
        } catch (MailException e) {
            log.error("Exception occurred when sending mail", e);
            throw new RuntimeException("Exception occurred when sending mail to springshop@email.com", e);
        }
    }
}
