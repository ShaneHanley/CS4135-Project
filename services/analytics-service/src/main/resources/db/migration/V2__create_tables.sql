-- Immutable event log consumed from the PGMQ queue; powers all aggregate queries (FR-17)
CREATE TABLE IF NOT EXISTS analytics_svc.prescription_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prescription_id UUID NOT NULL,
    pharmacy_id     TEXT NOT NULL,
    doctor_id       TEXT NOT NULL,
    patient_id      TEXT NOT NULL,
    medication_name TEXT NOT NULL,
    is_controlled   BOOLEAN NOT NULL DEFAULT false,
    status          TEXT NOT NULL,
    amount          NUMERIC(10, 2),
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_pe_pharmacy_occurred
    ON analytics_svc.prescription_events (pharmacy_id, occurred_at);

-- Pre-computed daily KPI snapshots per pharmacy; refreshed by the scheduled poller
CREATE TABLE IF NOT EXISTS analytics_svc.daily_kpi_snapshots (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pharmacy_id             TEXT NOT NULL,
    snapshot_date           DATE NOT NULL,
    total_prescriptions     INTEGER NOT NULL DEFAULT 0,
    total_revenue           NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    rejection_count         INTEGER NOT NULL DEFAULT 0,
    avg_processing_minutes  NUMERIC(8, 2),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (pharmacy_id, snapshot_date)
);
