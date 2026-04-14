CREATE TABLE IF NOT EXISTS prescription_svc.pharmacies (
  id      UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
  name    TEXT    NOT NULL,
  address TEXT    NOT NULL,
  phone   TEXT,
  active  BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO prescription_svc.pharmacies (id, name, address, phone) VALUES
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Central Pharmacy',    '1 Main Street, Limerick',      '061-100001'),
  ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'Northside Pharmacy',  '42 North Road, Limerick',      '061-100002'),
  ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'Southside Pharmacy',  '17 South Avenue, Limerick',    '061-100003'),
  ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'Campus Pharmacy',     'University of Limerick, V94',  '061-100004')
ON CONFLICT (id) DO NOTHING;
