package com.microobserve.incident.model;

import java.util.List;
import java.util.Map;

public record IncidentEvidence(
        String service,
        String alertName,
        String severity,
        Map<String, Double> metrics,
        List<String> recentErrors,
        List<String> traceSummaries,
        List<String> affectedDependencies) {
}
