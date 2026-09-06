package com.microobserve.incident.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class IncidentAnalysisTest {
    @Test
    void normalizesAlertTextBeforeStorageAndNotification() {
        var analysis = new IncidentAnalysis("incident\r\nInjected-Header: value",
                "invented", List.of("evidence"), null, null);
        assertThat(analysis.title()).doesNotContain("\r", "\n");
        assertThat(analysis.severity()).isEqualTo("WARNING");
        assertThat(analysis.affectedServices()).isEmpty();
        assertThat(analysis.recommendations()).isEmpty();
    }

    @Test
    void boundsEvidenceAndNormalizesSeverity() {
        var analysis = new IncidentAnalysis("incident", "critical",
                List.of("a".repeat(1500)), List.of("inventory-service"), List.of("inspect logs"));
        assertThat(analysis.evidence().getFirst()).hasSize(1000);
        assertThat(analysis.severity()).isEqualTo("CRITICAL");
    }
}
