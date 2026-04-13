-- One invoice per prescription once it reaches Ready for Pickup (FR-15)
CREATE TABLE IF NOT EXISTS billing_svc.invoices (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prescription_id UUID NOT NULL UNIQUE,
    patient_id      TEXT NOT NULL,
    pharmacy_id     TEXT NOT NULL,
    medication_name TEXT NOT NULL,
    amount          NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    status          TEXT NOT NULL DEFAULT 'UNPAID',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Payment recorded against an invoice by a Pharmacist or Manager (FR-16)
CREATE TABLE IF NOT EXISTS billing_svc.payments (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id     UUID NOT NULL REFERENCES billing_svc.invoices(id),
    amount_paid    NUMERIC(10, 2) NOT NULL,
    payment_method TEXT NOT NULL DEFAULT 'CASH',
    paid_by        TEXT NOT NULL,
    paid_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
