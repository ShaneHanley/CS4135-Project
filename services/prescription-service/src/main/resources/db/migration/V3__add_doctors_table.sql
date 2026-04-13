CREATE TABLE IF NOT EXISTS prescription_svc.doctors (
  id           UUID        PRIMARY KEY,
  first_name   TEXT        NOT NULL,
  last_name    TEXT        NOT NULL,
  email        TEXT        NOT NULL UNIQUE,
  license_number TEXT      NOT NULL UNIQUE,
  phone        TEXT,
  active       BOOLEAN     NOT NULL DEFAULT TRUE,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
