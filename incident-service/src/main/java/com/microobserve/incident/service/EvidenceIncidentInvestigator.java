package com.microobserve.incident.service;

import com.microobserve.incident.model.IncidentAnalysis;
import com.microobserve.incident.model.IncidentEvidence;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EvidenceIncidentInvestigator {

    public IncidentAnalysis investigate(IncidentEvidence evidence) {
        var findings = new ArrayList<String>();
        findings.addAll(evidence.collectionNotes());
        evidence.metrics().entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .filter(entry -> Double.isFinite(entry.getValue()))
                .forEach(entry -> findings.add(entry.getKey() + "=" + entry.getValue()));
        findings.addAll(evidence.recentErrors().stream().limit(3).toList());
        findings.addAll(evidence.traceSummaries().stream().limit(5).toList());
        if (evidence.metrics().isEmpty() && evidence.recentErrors().isEmpty()
                && evidence.traceSummaries().isEmpty()) {
            findings.add("No telemetry samples were collected. This does not confirm service health.");
        }

        return new IncidentAnalysis(
                evidence.alertName() + " detected in " + evidence.service(),
                evidence.severity(),
                List.copyOf(findings),
                List.of(evidence.service()),
                List.of("Inspect recent error logs and slow traces.",
                        "Review service latency, error rate, and database connection metrics.",
                        "Check recent changes and dependency health; the alert does not establish a root cause."));
    }
}
