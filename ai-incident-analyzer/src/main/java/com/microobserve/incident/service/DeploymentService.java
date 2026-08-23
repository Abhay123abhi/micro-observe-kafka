package com.microobserve.incident.service;

import com.microobserve.incident.config.IncidentProperties;
import com.microobserve.incident.model.DeploymentEvidence;
import com.microobserve.incident.model.DeploymentEvent;
import com.microobserve.incident.model.DeploymentRecord;
import com.microobserve.incident.repository.DeploymentRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DeploymentService {

    private final DeploymentRepository repository;
    private final OutboxService outbox;
    private final IncidentProperties properties;
    private final MeterRegistry meters;

    public DeploymentService(DeploymentRepository repository, OutboxService outbox,
                             IncidentProperties properties, MeterRegistry meters) {
        this.repository = repository;
        this.outbox = outbox;
        this.properties = properties;
        this.meters = meters;
    }

    @Transactional
    public DeploymentRecord record(String service, String environment, String version,
                                   String gitCommit, String changeSummary) {
        var deployment = new DeploymentRecord(UUID.randomUUID(), service, environment, version,
                blankToNull(gitCommit), blankToNull(changeSummary), Instant.now());
        repository.save(deployment);
        outbox.enqueue(deployment.id().toString(), properties.deploymentTopic(),
                new DeploymentEvent(deployment.id(), deployment.service(), deployment.environment(),
                        deployment.version(), deployment.gitCommit(), deployment.changeSummary(),
                        deployment.deployedAt()));
        meters.counter("deployment.recorded", "service", service, "environment", environment).increment();
        return deployment;
    }

    @Transactional(readOnly = true)
    public List<DeploymentEvidence> recentEvidence(String service) {
        Instant after = Instant.now().minus(properties.deploymentCorrelationWindow());
        return repository.findTop5ByServiceAndDeployedAtAfterOrderByDeployedAtDesc(service, after).stream()
                .map(deployment -> new DeploymentEvidence(deployment.version(), deployment.gitCommit(),
                        deployment.changeSummary(), deployment.deployedAt()))
                .toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
