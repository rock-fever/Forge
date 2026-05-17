CREATE TABLE clients (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    api_key     VARCHAR(255) NOT NULL UNIQUE,
    max_concurrent_jobs INT NOT NULL DEFAULT 5,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id       UUID NOT NULL REFERENCES clients(id),
    type            VARCHAR(100) NOT NULL,
    payload         JSONB,
    status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    priority        INT NOT NULL DEFAULT 0,
    retry_count     INT NOT NULL DEFAULT 0,
    max_retries     INT NOT NULL DEFAULT 3,
    result          TEXT,
    error_message   TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    started_at      TIMESTAMP WITH TIME ZONE,
    completed_at    TIMESTAMP WITH TIME ZONE,
    scheduled_after TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_jobs_status     ON jobs(status);
CREATE INDEX idx_jobs_client_id  ON jobs(client_id);
CREATE INDEX idx_jobs_created_at ON jobs(created_at DESC);

CREATE TABLE job_batches (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id      UUID NOT NULL REFERENCES clients(id),
    name           VARCHAR(255),
    total_jobs     INT NOT NULL DEFAULT 0,
    completed_jobs INT NOT NULL DEFAULT 0,
    failed_jobs    INT NOT NULL DEFAULT 0,
    status         VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at   TIMESTAMP WITH TIME ZONE
);

-- Tracks which jobs must finish before another job can start (for CompletableFuture chaining)
CREATE TABLE job_dependencies (
    job_id            UUID NOT NULL REFERENCES jobs(id),
    depends_on_job_id UUID NOT NULL REFERENCES jobs(id),
    PRIMARY KEY (job_id, depends_on_job_id)
);

ALTER TABLE jobs ADD COLUMN batch_id UUID REFERENCES job_batches(id);
CREATE INDEX idx_jobs_batch_id ON jobs(batch_id);
