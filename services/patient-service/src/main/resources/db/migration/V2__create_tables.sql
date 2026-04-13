-- Patient profiles (one per auth user with role PATIENT)
CREATE TABLE IF NOT EXISTS patient_svc.patient_profiles (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL UNIQUE,
    email               TEXT NOT NULL UNIQUE,
    first_name          TEXT NOT NULL,
    last_name           TEXT NOT NULL,
    phone               TEXT,
    date_of_birth       DATE,
    address_line1       TEXT,
    address_line2       TEXT,
    city                TEXT,
    delivery_preference TEXT NOT NULL DEFAULT 'PICKUP',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Refill requests raised by patients against an existing prescription
CREATE TABLE IF NOT EXISTS patient_svc.refill_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prescription_id UUID NOT NULL,
    patient_id      UUID NOT NULL,
    status          TEXT NOT NULL DEFAULT 'PENDING',
    notes           TEXT,
    requested_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    responded_at    TIMESTAMPTZ
);
