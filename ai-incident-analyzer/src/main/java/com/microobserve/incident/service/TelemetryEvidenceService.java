package com.microobserve.incident.service;

import com.microobserve.incident.api.AlertmanagerWebhook;
import com.microobserve.incident.config.IncidentProperties;
import com.microobserve.incident.model.IncidentEvidence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TelemetryEvidenceService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryEvidenceService.class);

    private final RestClient restClient;
    private final IncidentProperties properties;
    private final DeploymentService deployments;

    public TelemetryEvidenceService(RestClient.Builder restClient, IncidentProperties properties,
                                    DeploymentService deployments) {
        var httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = restClient.requestFactory(requestFactory).build();
        this.properties = properties;
        this.deployments = deployments;
    }

    public IncidentEvidence collect(AlertmanagerWebhook.Alert alert) {
        String service = alert.service();
        var metrics = new LinkedHashMap<String, Double>();

        queryMetric(metrics, "request_rate", "sum(rate(http_server_requests_seconds_count{job=\"%s\"}[5m]))".formatted(service));
        queryMetric(metrics, "error_rate", "sum(rate(http_server_requests_seconds_count{job=\"%s\",status=~\"5..\"}[5m]))".formatted(service));
        queryMetric(metrics, "p95_latency_seconds", "histogram_quantile(0.95,sum by(le)(rate(http_server_requests_seconds_bucket{job=\"%s\"}[5m])))".formatted(service));
        queryMetric(metrics, "active_db_connections", "sum(hikaricp_connections_active{job=\"%s\"})".formatted(service));

        return new IncidentEvidence(service, alert.alertName(), alert.severity(), Map.copyOf(metrics),
                fetchLogs(service), fetchTraces(service), dependencies(service), deployments.recentEvidence(service));
    }

    private void queryMetric(Map<String, Double> metrics, String name, String query) {
        try {
            JsonNode response = restClient.get().uri(properties.prometheusUrl() + "/api/v1/query?query={query}", query)
                    .retrieve().body(JsonNode.class);
            JsonNode result = response.path("data").path("result");
            if (!result.isEmpty()) {
                metrics.put(name, result.get(0).path("value").get(1).asDouble());
            }
        } catch (Exception exception) {
            log.warn("Unable to collect metric {}: {}", name, exception.getMessage());
        }
    }

    private List<String> fetchLogs(String service) {
        try {
            String query = "{service=\"%s\"} |~ \"(?i)error|exception|timeout\"".formatted(service);
            JsonNode response = restClient.get()
                    .uri(properties.lokiUrl() + "/loki/api/v1/query_range?query={query}&limit={limit}",
                            query, properties.maximumLogLines())
                    .retrieve().body(JsonNode.class);

            var lines = new ArrayList<String>();
            response.path("data").path("result").forEach(stream -> stream.path("values")
                    .forEach(value -> lines.add(redact(value.get(1).asText()))));
            return lines.stream().distinct().limit(properties.maximumLogLines()).toList();
        } catch (Exception exception) {
            log.warn("Unable to collect logs for {}: {}", service, exception.getMessage());
            return List.of();
        }
    }

    private List<String> fetchTraces(String service) {
        try {
            JsonNode response = restClient.get()
                    .uri(properties.tempoUrl() + "/api/search?tags=service.name%3D{service}&limit=5", service)
                    .retrieve().body(JsonNode.class);
            var traces = new ArrayList<String>();
            response.path("traces").forEach(trace -> traces.add("traceId=%s durationMs=%s".formatted(
                    trace.path("traceID").asText(), trace.path("durationMs").asText())));
            return List.copyOf(traces);
        } catch (Exception exception) {
            log.warn("Unable to collect traces for {}: {}", service, exception.getMessage());
            return List.of();
        }
    }

    private static List<String> dependencies(String service) {
        return switch (service) {
            case "order-service" -> List.of("order-service", "inventory-service", "api-gateway", "postgresql");
            case "inventory-service" -> List.of("inventory-service", "order-service", "api-gateway", "postgresql");
            case "notification-service" -> List.of("notification-service", "kafka", "smtp");
            default -> List.of(service);
        };
    }

    private static String redact(String value) {
        String redacted = value.replaceAll("(?i)(password|token|secret|api[_-]?key|authorization)\\s*[=:]\\s*[^\\s,]+", "$1=[REDACTED]")
                .replaceAll("(?i)bearer\\s+[A-Za-z0-9._~+/=-]+", "Bearer [REDACTED]")
                .replaceAll("\\bsk-[A-Za-z0-9_-]{16,}\\b", "[API_KEY_REDACTED]")
                .replaceAll("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}", "[EMAIL_REDACTED]");
        return redacted.length() <= 1000 ? redacted : redacted.substring(0, 1000) + "[TRUNCATED]";
    }
}
