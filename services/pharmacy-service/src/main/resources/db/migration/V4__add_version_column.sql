ALTER TABLE pharmacy_svc.pharmacy_prescriptions
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
