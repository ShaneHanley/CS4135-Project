INSERT INTO auth_svc.users (id, first_name, last_name, email, password_hash, role, active)
VALUES
  ('11111111-1111-1111-1111-111111111111', 'Demo', 'Doctor', 'doctor@demo.com', '***REMOVED***', 'DOCTOR', true),
  ('22222222-2222-2222-2222-222222222222', 'Demo', 'Pharmacist', 'pharmacist@demo.com', '***REMOVED***', 'PHARMACIST', true),
  ('33333333-3333-3333-3333-333333333333', 'Demo', 'Manager', 'manager@demo.com', '***REMOVED***', 'MANAGER', true)
ON CONFLICT (email) DO NOTHING;
