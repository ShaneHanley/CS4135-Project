INSERT INTO pharmacy_svc.pharmacy_prescriptions (
  id, prescription_id, doctor_id, patient_id, patient_email, patient_name,
  pharmacy_id, medication_name, dosage, quantity, status
)
VALUES (
  '66666666-6666-6666-6666-666666666666',
  '44444444-4444-4444-4444-444444444444',
  '11111111-1111-1111-1111-111111111111',
  '55555555-5555-5555-5555-555555555555',
  'patient@demo.com',
  'Demo Patient',
  'default-pharmacy',
  'Amoxicillin',
  '500mg',
  21,
  'NEW'
)
ON CONFLICT (prescription_id) DO NOTHING;
