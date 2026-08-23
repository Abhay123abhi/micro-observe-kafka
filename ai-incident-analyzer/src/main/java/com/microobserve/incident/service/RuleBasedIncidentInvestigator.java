package com.microobserve.incident.service;

import com.microobserve.incident.model.IncidentAnalysis;
import com.microobserve.incident.model.IncidentEvidence;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RuleBasedIncidentInvestigator implements IncidentInvestigator {

    @Override
    public IncidentAnalysis investigate(IncidentEvidence evidence) {
        var findings = new ArrayList<String>();
        evidence.metrics().forEach((metric, value) -> findings.add(metric + "=" + value));
        findings.addAll(evidence.recentErrors().stream().limit(3).toList());

        String rootCause = evidence.recentErrors().stream().findFirst()
                .orElse("The alert condition indicates abnormal service behavior; inspect its telemetry.");

        return new IncidentAnalysis(
                evidence.alertName() + " detected in " + evidence.service(),
                rootCause,
                evidence.severity().toUpperCase(),
                findings.isEmpty() ? 0.45 : 0.72,
                List.copyOf(findings),
                evidence.affectedDependencies(),
                List.of("Inspect recent error logs and slow traces.",
                        "Review service latency, error rate, and database connection metrics.",
                        "Check the most recent deployment and dependency health."));
    }
}
