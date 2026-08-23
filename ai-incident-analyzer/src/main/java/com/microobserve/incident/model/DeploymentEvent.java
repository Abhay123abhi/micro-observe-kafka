package com.microobserve.incident.model;

import java.time.Instant;
import java.util.UUID;

public record DeploymentEvent(
        UUID id,
        String service,
        String environment,
        String version,
        String gitCommit,
        String changeSummary,
        Instant deployedAt) {
}
