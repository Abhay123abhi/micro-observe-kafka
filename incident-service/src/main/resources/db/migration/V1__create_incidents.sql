CREATE TABLE incidents (
    id VARCHAR(36) PRIMARY KEY,
    fingerprint VARCHAR(255) NOT NULL,
    service VARCHAR(100) NOT NULL,
    alert_name VARCHAR(150) NOT NULL,
    status VARCHAR(40) NOT NULL,
    severity VARCHAR(40),
    title VARCHAR(255),
    probable_root_cause TEXT,
    confidence DOUBLE PRECISION NOT NULL DEFAULT 0,
    evidence JSONB NOT NULL DEFAULT '[]',
    affected_services JSONB NOT NULL DEFAULT '[]',
    recommendations JSONB NOT NULL DEFAULT '[]',
    detected_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_incidents_status_detected ON incidents (status, detected_at DESC);
CREATE INDEX idx_incidents_service_detected ON incidents (service, detected_at DESC);
CREATE UNIQUE INDEX uk_incidents_active_fingerprint ON incidents (fingerprint) WHERE status <> 'RESOLVED';

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(36) NOT NULL,
    topic VARCHAR(150) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;
