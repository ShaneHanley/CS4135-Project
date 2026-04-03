CREATE SCHEMA IF NOT EXISTS prescription_svc;
CREATE TABLE IF NOT EXISTS prescription_svc.prescriptions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  doctor_id TEXT NOT NULL,
  patient_id TEXT NOT NULL,
  patient_email TEXT NOT NULL,
  patient_name TEXT NOT NULL,
  pharmacy_id TEXT NOT NULL,
  medication_name TEXT NOT NULL,
  dosage TEXT NOT NULL,
  instructions TEXT NOT NULL,
  quantity INTEGER NOT NULL,
  status TEXT NOT NULL DEFAULT 'NEW',
  rejection_reason TEXT,
  refills_allowed INTEGER NOT NULL DEFAULT 0,
  refills_used INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
