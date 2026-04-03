INSERT INTO prescription_svc.prescriptions (
  id, doctor_id, patient_id, patient_email, patient_name, pharmacy_id,
  medication_name, dosage, instructions, quantity, status, refills_allowed, refills_used
)
VALUES (
  '44444444-4444-4444-4444-444444444444',
  '11111111-1111-1111-1111-111111111111',
  '55555555-5555-5555-5555-555555555555',
  'patient@demo.com',
  'Demo Patient',
  'default-pharmacy',
  'Amoxicillin',
  '500mg',
  'Take one capsule every 8 hours',
  21,
  'NEW',
  1,
  0
)
ON CONFLICT (id) DO NOTHING;
