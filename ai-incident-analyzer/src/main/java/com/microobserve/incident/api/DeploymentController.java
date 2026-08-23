package com.microobserve.incident.api;

import com.microobserve.incident.model.DeploymentRecord;
import com.microobserve.incident.service.DeploymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/deployments")
@Validated
public class DeploymentController {

    private final DeploymentService deploymentService;

    public DeploymentController(DeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeploymentView record(@Valid @RequestBody DeploymentRequest request) {
        return DeploymentView.from(deploymentService.record(request.service(), request.environment(),
                request.version(), request.gitCommit(), request.changeSummary()));
    }

    public record DeploymentRequest(
            @NotBlank @Pattern(regexp = "[a-z0-9-]{1,100}") String service,
            @NotBlank @Pattern(regexp = "[a-z0-9-]{1,40}") String environment,
            @NotBlank @Size(max = 100) String version,
            @Size(max = 128) String gitCommit,
            @Size(max = 1_000) String changeSummary) {
    }

    public record DeploymentView(
            UUID id,
            String service,
            String environment,
            String version,
            String gitCommit,
            String changeSummary,
            Instant deployedAt) {

        static DeploymentView from(DeploymentRecord deployment) {
            return new DeploymentView(deployment.id(), deployment.service(), deployment.environment(),
                    deployment.version(), deployment.gitCommit(), deployment.changeSummary(),
                    deployment.deployedAt());
        }
    }
}
