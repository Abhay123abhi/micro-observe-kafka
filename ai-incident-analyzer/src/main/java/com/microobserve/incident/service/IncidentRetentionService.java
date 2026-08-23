package com.microobserve.incident.service;

import com.microobserve.incident.config.IncidentProperties;
import com.microobserve.incident.model.IncidentStatus;
import com.microobserve.incident.repository.IncidentRepository;
import com.microobserve.incident.repository.OutboxRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class IncidentRetentionService {

    private static final Logger log = LoggerFactory.getLogger(IncidentRetentionService.class);

    private final IncidentRepository incidents;
    private final OutboxRepository outbox;
    private final IncidentProperties properties;

    public IncidentRetentionService(IncidentRepository incidents, OutboxRepository outbox,
                                    IncidentProperties properties) {
        this.incidents = incidents;
        this.outbox = outbox;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${incident.retention-cleanup-delay:1h}",
            initialDelayString = "${incident.retention-cleanup-initial-delay:5m}")
    @Transactional
    public void removeExpiredRecords() {
        Instant now = Instant.now();
        long removedIncidents = incidents.deleteByStatusAndResolvedAtBefore(IncidentStatus.RESOLVED,
                now.minus(properties.resolvedRetentionDays(), ChronoUnit.DAYS));
        long removedOutboxEvents = outbox.deleteByPublishedAtBefore(
                now.minus(properties.outboxRetentionDays(), ChronoUnit.DAYS));

        if (removedIncidents > 0 || removedOutboxEvents > 0) {
            log.info("Retention removed {} resolved incidents and {} published outbox events",
                    removedIncidents, removedOutboxEvents);
        }
    }
}
