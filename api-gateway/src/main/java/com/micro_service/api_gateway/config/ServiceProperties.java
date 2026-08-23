package com.micro_service.api_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties("services")
public record ServiceProperties(Service product, Service order, Service inventory) {

    public record Service(URI url) {
    }
}
