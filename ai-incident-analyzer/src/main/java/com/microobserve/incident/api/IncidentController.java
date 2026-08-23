package com.microobserve.incident.api;

import com.microobserve.incident.model.IncidentRecord;
import com.microobserve.incident.model.IncidentStatus;
import com.microobserve.incident.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping("/webhooks/alertmanager")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.ACCEPTED)
    public List<IncidentAccepted> receiveAlert(@Valid @RequestBody AlertmanagerWebhook webhook) {
        return webhook.alerts().stream()
                .map(incidentService::receive)
                .map(IncidentAccepted::new)
                .toList();
    }

    @GetMapping
    public Collection<IncidentView> activeIncidents() {
        return incidentService.activeIncidents().stream().map(IncidentView::from).toList();
    }

    public record IncidentAccepted(String incidentId) {
    }

    public record IncidentView(
            String incidentId,
            String service,
            String alertName,
            IncidentStatus status,
            String severity,
            String title,
            String probableRootCause,
            double confidence,
            List<String> evidence,
            List<String> affectedServices,
            List<String> recommendations,
            java.time.Instant detectedAt,
            java.time.Instant updatedAt) {

        static IncidentView from(IncidentRecord incident) {
            return new IncidentView(incident.id(), incident.service(), incident.alertName(), incident.status(),
                    incident.severity(), incident.title(), incident.probableRootCause(), incident.confidence(),
                    incident.evidence(), incident.affectedServices(), incident.recommendations(),
                    incident.detectedAt(), incident.updatedAt());
        }
    }
}
