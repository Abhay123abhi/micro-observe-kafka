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
        evidence.recentDeployments().stream().findFirst().ifPresent(deployment ->
                findings.add("recent_deployment version=%s commit=%s deployedAt=%s".formatted(
                        deployment.version(), deployment.gitCommit(), deployment.deployedAt())));

        String rootCause = evidence.recentErrors().stream().findFirst()
                .orElseGet(() -> evidence.recentDeployments().stream().findFirst()
                        .map(deployment -> ("A recent deployment (%s) is correlated with the incident. "
                                + "Review its change summary and commit; correlation is not proof of causation.")
                                .formatted(deployment.version()))
                        .orElse("The alert condition indicates abnormal service behavior; inspect its telemetry."));

        return new IncidentAnalysis(
                evidence.alertName() + " detected in " + evidence.service(),
                rootCause,
                evidence.severity().toUpperCase(),
                findings.isEmpty() ? 0.45 : 0.72,
                List.copyOf(findings),
                evidence.affectedDependencies(),
                List.of("Inspect recent error logs and slow traces.",
                        "Review service latency, error rate, and database connection metrics.",
                        "Review the correlated deployment before considering rollback."));
    }
}
