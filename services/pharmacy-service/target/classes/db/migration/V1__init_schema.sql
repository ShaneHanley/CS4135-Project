CREATE SCHEMA IF NOT EXISTS pharmacy_svc;
CREATE TABLE IF NOT EXISTS pharmacy_svc.pharmacy_prescriptions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  prescription_id UUID NOT NULL UNIQUE,
  doctor_id TEXT NOT NULL,
  patient_id TEXT NOT NULL,
  patient_email TEXT NOT NULL,
  patient_name TEXT NOT NULL,
  pharmacy_id TEXT NOT NULL,
  medication_name TEXT NOT NULL,
  dosage TEXT NOT NULL,
  quantity INTEGER NOT NULL,
  status TEXT NOT NULL DEFAULT 'NEW',
  rejection_reason TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
