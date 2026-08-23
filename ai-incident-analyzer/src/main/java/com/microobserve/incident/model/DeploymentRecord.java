package com.microobserve.incident.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "deployments")
public class DeploymentRecord {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String service;

    @Column(nullable = false, length = 40)
    private String environment;

    @Column(nullable = false, length = 100)
    private String version;

    @Column(length = 128)
    private String gitCommit;

    @Column(length = 1_000)
    private String changeSummary;

    @Column(nullable = false, updatable = false)
    private Instant deployedAt;

    protected DeploymentRecord() {
    }

    public DeploymentRecord(UUID id, String service, String environment, String version,
                            String gitCommit, String changeSummary, Instant deployedAt) {
        this.id = id;
        this.service = service;
        this.environment = environment;
        this.version = version;
        this.gitCommit = gitCommit;
        this.changeSummary = changeSummary;
        this.deployedAt = deployedAt;
    }

    public UUID id() { return id; }
    public String service() { return service; }
    public String environment() { return environment; }
    public String version() { return version; }
    public String gitCommit() { return gitCommit; }
    public String changeSummary() { return changeSummary; }
    public Instant deployedAt() { return deployedAt; }
}
