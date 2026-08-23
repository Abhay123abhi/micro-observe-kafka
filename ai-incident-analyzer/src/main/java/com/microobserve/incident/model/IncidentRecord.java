package com.microobserve.incident.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "incidents")
public class IncidentRecord {

    @Id
    private String id;

    @Column(nullable = false)
    private String fingerprint;

    @Column(nullable = false)
    private String service;

    @Column(nullable = false)
    private String alertName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status;

    private String severity;
    private String title;

    @Column(columnDefinition = "text")
    private String probableRootCause;

    private double confidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> evidence = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> affectedServices = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> recommendations = List.of();

    @Column(nullable = false, updatable = false)
    private Instant detectedAt;
    private Instant updatedAt;
    private Instant resolvedAt;

    @Version
    private long version;

    protected IncidentRecord() {
    }

    public IncidentRecord(String id, String fingerprint, String service, String alertName,
                          String severity, Instant detectedAt) {
        this.id = id;
        this.fingerprint = fingerprint;
        this.service = service;
        this.alertName = alertName;
        this.severity = severity;
        this.status = IncidentStatus.RECEIVED;
        this.detectedAt = detectedAt;
        this.updatedAt = detectedAt;
    }

    public void startInvestigation() {
        status = IncidentStatus.INVESTIGATING;
        updatedAt = Instant.now();
    }

    public void complete(IncidentAnalysis analysis) {
        title = analysis.title();
        probableRootCause = analysis.probableRootCause();
        severity = analysis.severity();
        confidence = analysis.confidence();
        evidence = List.copyOf(analysis.evidence());
        affectedServices = List.copyOf(analysis.affectedServices());
        recommendations = List.copyOf(analysis.recommendations());
        status = IncidentStatus.INVESTIGATED;
        updatedAt = Instant.now();
    }

    public void failInvestigation() {
        status = IncidentStatus.INVESTIGATION_FAILED;
        updatedAt = Instant.now();
    }

    public void resolve() {
        status = IncidentStatus.RESOLVED;
        resolvedAt = Instant.now();
        updatedAt = resolvedAt;
    }

    public String id() { return id; }
    public String fingerprint() { return fingerprint; }
    public String service() { return service; }
    public String alertName() { return alertName; }
    public IncidentStatus status() { return status; }
    public String severity() { return severity; }
    public String title() { return title; }
    public String probableRootCause() { return probableRootCause; }
    public double confidence() { return confidence; }
    public List<String> evidence() { return evidence; }
    public List<String> affectedServices() { return affectedServices; }
    public List<String> recommendations() { return recommendations; }
    public Instant detectedAt() { return detectedAt; }
    public Instant updatedAt() { return updatedAt; }
    public Instant resolvedAt() { return resolvedAt; }
}
