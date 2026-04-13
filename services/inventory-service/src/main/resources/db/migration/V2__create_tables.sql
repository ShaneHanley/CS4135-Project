-- Master catalogue of medications
CREATE TABLE IF NOT EXISTS inventory_svc.medications (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          TEXT NOT NULL UNIQUE,
    description   TEXT,
    unit          TEXT NOT NULL DEFAULT 'tablet',
    is_controlled BOOLEAN NOT NULL DEFAULT false,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Lot-based stock per medication per pharmacy (FR-10)
CREATE TABLE IF NOT EXISTS inventory_svc.inventory_lots (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pharmacy_id       TEXT NOT NULL,
    medication_id     UUID NOT NULL REFERENCES inventory_svc.medications(id),
    lot_number        TEXT NOT NULL,
    expiry_date       DATE,
    quantity          INTEGER NOT NULL DEFAULT 0,
    reorder_threshold INTEGER NOT NULL DEFAULT 10,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (pharmacy_id, medication_id, lot_number)
);

-- Audit trail of every stock movement (FR-11: dispense deductions, FR-12: receive additions)
CREATE TABLE IF NOT EXISTS inventory_svc.inventory_transactions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lot_id           UUID NOT NULL REFERENCES inventory_svc.inventory_lots(id),
    pharmacy_id      TEXT NOT NULL,
    medication_id    UUID NOT NULL REFERENCES inventory_svc.medications(id),
    transaction_type TEXT NOT NULL,
    quantity_change  INTEGER NOT NULL,
    prescription_id  UUID,
    performed_by     TEXT NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
