ALTER TABLE prescription_svc.prescriptions
  ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128);

CREATE UNIQUE INDEX IF NOT EXISTS ux_prescriptions_doctor_idempotency
  ON prescription_svc.prescriptions (doctor_id, idempotency_key)
  WHERE idempotency_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS prescription_svc.outbox_events (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  queue_name TEXT NOT NULL,
  payload TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  published_at TIMESTAMPTZ,
  last_error TEXT
);

CREATE INDEX IF NOT EXISTS ix_outbox_unpublished
  ON prescription_svc.outbox_events (created_at)
  WHERE published_at IS NULL;
