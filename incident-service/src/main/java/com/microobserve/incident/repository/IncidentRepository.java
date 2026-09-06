package com.microobserve.incident.repository;

import com.microobserve.incident.model.IncidentRecord;
import com.microobserve.incident.model.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<IncidentRecord, String> {
    Optional<IncidentRecord> findByFingerprintAndStatusNot(String fingerprint, IncidentStatus status);
    Optional<IncidentRecord> findFirstByFingerprintOrderByDetectedAtDesc(String fingerprint);
    Page<IncidentRecord> findAllByStatusNotOrderByDetectedAtDesc(IncidentStatus status, Pageable pageable);
    Page<IncidentRecord> findAllByStatusOrderByDetectedAtDesc(IncidentStatus status, Pageable pageable);
    long countByStatusNot(IncidentStatus status);
    long deleteByStatusAndResolvedAtBefore(IncidentStatus status, Instant cutoff);
}
