CREATE TABLE deployments (
    id UUID PRIMARY KEY,
    service VARCHAR(100) NOT NULL,
    environment VARCHAR(40) NOT NULL,
    version VARCHAR(100) NOT NULL,
    git_commit VARCHAR(128),
    change_summary VARCHAR(1000),
    deployed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_deployments_service_deployed
    ON deployments (service, deployed_at DESC);
