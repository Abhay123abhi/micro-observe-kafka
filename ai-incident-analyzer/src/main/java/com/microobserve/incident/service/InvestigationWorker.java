package com.microobserve.incident.service;

import com.microobserve.incident.model.InvestigationRequested;
import com.microobserve.incident.repository.IncidentRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class InvestigationWorker {

    private static final Logger log = LoggerFactory.getLogger(InvestigationWorker.class);

    private final IncidentRepository repository;
    private final TelemetryEvidenceService evidenceService;
    private final IncidentInvestigator investigator;
    private final InvestigationResultService resultService;
    private final MeterRegistry meters;

    public InvestigationWorker(IncidentRepository repository, TelemetryEvidenceService evidenceService,
                               IncidentInvestigator investigator, InvestigationResultService resultService,
                               MeterRegistry meters) {
        this.repository = repository;
        this.evidenceService = evidenceService;
        this.investigator = investigator;
        this.resultService = resultService;
        this.meters = meters;
    }

    @KafkaListener(topics = "${incident.investigation-topic:incident-investigation}",
            groupId = "incident-investigation-worker")
    public void investigate(InvestigationRequested request) {
        var timer = Timer.start(meters);
        var incident = repository.findById(request.incidentId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown incident " + request.incidentId()));
        incident.startInvestigation();
        repository.save(incident);

        try {
            var analysis = investigator.investigate(evidenceService.collect(request.alert()));
            resultService.complete(incident.id(), analysis);
            meters.counter("incident.investigated", "service", incident.service(), "severity", analysis.severity()).increment();
            meters.summary("incident.confidence", "service", incident.service()).record(analysis.confidence());
            log.info("Completed investigation {} for {}", incident.id(), incident.service());
        } catch (RuntimeException exception) {
            resultService.fail(incident.id());
            meters.counter("incident.investigation.failure", "service", incident.service()).increment();
            log.error("Investigation {} failed", incident.id(), exception);
        } finally {
            timer.stop(meters.timer("incident.investigation.duration", "service", incident.service()));
        }
    }
}
