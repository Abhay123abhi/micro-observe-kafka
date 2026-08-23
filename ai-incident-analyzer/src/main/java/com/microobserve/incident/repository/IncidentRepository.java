package com.microobserve.incident.repository;

import com.microobserve.incident.model.IncidentRecord;
import com.microobserve.incident.model.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<IncidentRecord, String> {
    Optional<IncidentRecord> findByFingerprintAndStatusNot(String fingerprint, IncidentStatus status);
    Optional<IncidentRecord> findFirstByFingerprintOrderByDetectedAtDesc(String fingerprint);
    List<IncidentRecord> findAllByStatusNotOrderByDetectedAtDesc(IncidentStatus status);
    long countByStatusNot(IncidentStatus status);
}
