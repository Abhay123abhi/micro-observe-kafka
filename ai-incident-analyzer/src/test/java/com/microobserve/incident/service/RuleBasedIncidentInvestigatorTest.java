package com.microobserve.incident.service;

import com.microobserve.incident.model.IncidentEvidence;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedIncidentInvestigatorTest {

    private final RuleBasedIncidentInvestigator investigator = new RuleBasedIncidentInvestigator();

    @Test
    void createsActionableAnalysisWithoutAnAiProvider() {
        var evidence = new IncidentEvidence("order-service", "HighErrorRate", "critical",
                Map.of("error_rate", 0.12), List.of("Database connection timed out"), List.of(),
                List.of("order-service", "inventory-service"));

        var analysis = investigator.investigate(evidence);

        assertThat(analysis.severity()).isEqualTo("CRITICAL");
        assertThat(analysis.probableRootCause()).contains("Database connection timed out");
        assertThat(analysis.affectedServices()).contains("inventory-service");
        assertThat(analysis.recommendations()).isNotEmpty();
    }
}
