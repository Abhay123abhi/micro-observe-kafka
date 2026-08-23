package com.microobserve.incident.model;

import java.time.Instant;

public record DeploymentEvidence(
        String version,
        String gitCommit,
        String changeSummary,
        Instant deployedAt) {
}
