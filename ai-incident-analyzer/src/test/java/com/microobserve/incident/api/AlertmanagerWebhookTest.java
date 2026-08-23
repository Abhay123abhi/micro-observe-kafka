package com.microobserve.incident.api;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AlertmanagerWebhookTest {

    @Test
    void rejectsQueryControlCharactersInServiceLabels() {
        var alert = new AlertmanagerWebhook.Alert("firing",
                Map.of("service", "inventory-service\"} or up{job=\"postgres"),
                Map.of(), null, null, "fingerprint");

        assertThat(alert.service()).isEqualTo("unknown");
    }

    @Test
    void normalizesUnknownSeverities() {
        var alert = new AlertmanagerWebhook.Alert("firing",
                Map.of("service", "inventory-service", "severity", "unexpected"),
                Map.of(), null, null, "fingerprint");

        assertThat(alert.service()).isEqualTo("inventory-service");
        assertThat(alert.severity()).isEqualTo("warning");
    }
}
