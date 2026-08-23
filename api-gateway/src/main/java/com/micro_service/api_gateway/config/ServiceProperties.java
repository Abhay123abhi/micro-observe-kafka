package com.micro_service.api_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties("services")
public record ServiceProperties(Service order, Service inventory, Service incident) {

    public record Service(URI url) {
    }
}
