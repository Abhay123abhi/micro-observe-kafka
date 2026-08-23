package com.microobserve.incident.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentAnalysisTest {

    @Test
    void boundsUntrustedModelOutputBeforeStorageAndNotification() {
        var analysis = new IncidentAnalysis("incident\r\nInjected-Header: value", null,
                "invented", Double.NaN, List.of("evidence"), null, null);

        assertThat(analysis.title()).doesNotContain("\r", "\n");
        assertThat(analysis.severity()).isEqualTo("WARNING");
        assertThat(analysis.confidence()).isZero();
        assertThat(analysis.affectedServices()).isEmpty();
        assertThat(analysis.recommendations()).isEmpty();
    }

    @Test
    void clampsConfidenceAndLimitsOversizedEvidence() {
        var analysis = new IncidentAnalysis("incident", "cause", "critical", 4.5,
                List.of("a".repeat(1500)), List.of("inventory-service"), List.of("retry"));

        assertThat(analysis.confidence()).isEqualTo(1.0);
        assertThat(analysis.evidence().getFirst()).hasSize(1000);
        assertThat(analysis.severity()).isEqualTo("CRITICAL");
    }
}
