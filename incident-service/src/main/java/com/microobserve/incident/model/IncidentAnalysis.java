package com.microobserve.incident.model;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public record IncidentAnalysis(
        String title,
        String severity,
        List<String> evidence,
        List<String> affectedServices,
        List<String> recommendations) {

    private static final Set<String> SEVERITIES = Set.of("INFO", "WARNING", "CRITICAL");

    public IncidentAnalysis {
        title = normalize(title, "Production incident", 255);
        String normalizedSeverity = severity == null ? "WARNING" : severity.toUpperCase(Locale.ROOT);
        severity = SEVERITIES.contains(normalizedSeverity) ? normalizedSeverity : "WARNING";
        evidence = bounded(evidence);
        affectedServices = bounded(affectedServices);
        recommendations = bounded(recommendations);
    }

    private static List<String> bounded(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .limit(20)
                .map(value -> normalize(value, "", 1000))
                .toList();
    }

    private static String normalize(String value, String fallback, int maximumLength) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String sanitized = value.replace('\r', ' ').replace('\n', ' ').strip();
        return sanitized.length() <= maximumLength ? sanitized : sanitized.substring(0, maximumLength);
    }
}
