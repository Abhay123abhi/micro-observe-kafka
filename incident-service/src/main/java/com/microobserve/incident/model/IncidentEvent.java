package com.microobserve.incident.model;

import java.time.Instant;
import java.util.List;

public record IncidentEvent(
        String incidentId,
        String fingerprint,
        String status,
        String service,
        String alertName,
        String title,
        String severity,
        List<String> evidence,
        List<String> affectedServices,
        List<String> recommendations,
        String dashboardUrl,
        Instant occurredAt) {
}
