package com.micro_service.notification_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("notification.mail")
public record NotificationProperties(boolean enabled, String from, String recipient) {
}
