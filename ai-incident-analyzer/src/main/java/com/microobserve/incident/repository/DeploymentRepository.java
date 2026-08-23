package com.microobserve.incident.repository;

import com.microobserve.incident.model.DeploymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DeploymentRepository extends JpaRepository<DeploymentRecord, UUID> {

    List<DeploymentRecord> findTop5ByServiceAndDeployedAtAfterOrderByDeployedAtDesc(
            String service, Instant deployedAfter);
}
