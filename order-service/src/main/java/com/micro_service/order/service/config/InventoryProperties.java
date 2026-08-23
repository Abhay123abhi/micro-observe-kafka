package com.micro_service.order.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties("inventory")
public record InventoryProperties(URI url) {
}
