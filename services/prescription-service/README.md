# prescription-service

Schema: `prescription_svc`  
Port: `8082`

## MVP scope
- Doctor creates and tracks prescriptions.
- Prescriptions persisted in Supabase schema `prescription_svc`.
- On create, a row is written to the transactional **outbox** (`outbox_events`) in the same database transaction as the prescription. An `OutboxPublisherScheduler` runs every **2 seconds**, reads unpublished rows, and calls `pgmq.send` for queue `prescription_created`, then sets `published_at`. Delivery to PGMQ is therefore **asynchronous** (typically within a few seconds).

## Idempotency
- `POST /api/doctor/prescriptions` requires header **`X-Idempotency-Key`** (client-generated, max 128 characters, e.g. a UUID).
- The key is stored on `prescriptions.idempotency_key` with a unique constraint per doctor. A duplicate POST with the same key for the same authenticated doctor returns the **existing** prescription without creating a duplicate row or outbox entry.

## Endpoints
- `GET /api/doctor/prescriptions`
- `POST /api/doctor/prescriptions` (requires `X-Idempotency-Key`)
- `GET /api/doctor/prescriptions/{id}/status`
- `GET /api/doctor/pharmacies`

## Queue contract published
`prescription_created` payload:
- `prescriptionId`
- `doctorId`
- `patientId`
- `patientEmail`
- `patientName`
- `pharmacyId`
- `medicationName`
- `dosage`
- `quantity`
- `createdAt`
