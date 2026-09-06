package com.microobserve.incident.service;

import com.microobserve.incident.model.IncidentEvidence;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class EvidenceIncidentInvestigatorTest {
    private final EvidenceIncidentInvestigator investigator = new EvidenceIncidentInvestigator();

    @Test
    void includesMetricsErrorsAndTracesWithoutInferringDependencyFailures() {
        var evidence = new IncidentEvidence("order-service", "HighErrorRate", "critical",
                Map.of("error_requests_per_second", 0.12), List.of("Database connection timed out"),
                List.of("traceId=abc durationMs=3000"), List.of());
        var analysis = investigator.investigate(evidence);
        assertThat(analysis.severity()).isEqualTo("CRITICAL");
        assertThat(analysis.evidence()).contains("Database connection timed out",
                "traceId=abc durationMs=3000", "error_requests_per_second=0.12");
        assertThat(analysis.affectedServices()).containsExactly("order-service");
    }

    @Test
    void distinguishesMissingEvidenceFromHealthyService() {
        var evidence = new IncidentEvidence("inventory-service", "ServiceUnavailable", "critical",
                Map.of(), List.of(), List.of(), List.of("Loki unavailable: error logs could not be collected."));
        var analysis = investigator.investigate(evidence);
        assertThat(analysis.evidence()).contains(
                "Loki unavailable: error logs could not be collected.",
                "No telemetry samples were collected. This does not confirm service health.");
    }
}
