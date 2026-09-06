package com.microobserve.incident.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public record AlertmanagerWebhook(String status, @NotEmpty @Size(max = 50) List<@Valid Alert> alerts) {

    public record Alert(
            String status,
            @Size(max = 50) Map<@Size(max = 100) String, @Size(max = 512) String> labels,
            @Size(max = 50) Map<@Size(max = 100) String, @Size(max = 2048) String> annotations,
            String startsAt,
            String endsAt,
            @Size(max = 128) String fingerprint) {

        private static final Pattern SAFE_LABEL = Pattern.compile("[A-Za-z0-9_.:-]{1,100}");
        private static final Set<String> SEVERITIES = Set.of("info", "warning", "critical");

        public String service() {
            String value = labels == null
                    ? "unknown"
                    : labels.getOrDefault("service", labels.getOrDefault("job", "unknown"));
            return value != null && SAFE_LABEL.matcher(value).matches() ? value : "unknown";
        }

        public String alertName() {
            String value = labels == null ? "unknown" : labels.getOrDefault("alertname", "unknown");
            return value != null && SAFE_LABEL.matcher(value).matches() ? value : "unknown";
        }

        public String severity() {
            String value = labels == null ? "warning" : labels.getOrDefault("severity", "warning");
            String normalized = value == null ? "warning" : value.toLowerCase(Locale.ROOT);
            return SEVERITIES.contains(normalized) ? normalized : "warning";
        }
    }
}
