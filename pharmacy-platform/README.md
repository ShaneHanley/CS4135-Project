# Pharmacy Management Platform (MVP)

## Included services
- `gateway-service` (JWT validation + header injection)
- `auth-service` (Supabase-backed users + JWT)
- `prescription-service` (doctor prescription lifecycle start)
- `pharmacy-service` (queue consumer + status updates)
- `notification-service` (existing service, integrated)
- supporting placeholder services: `patient`, `inventory`, `billing`, `analytics`
- `web-frontend`

## Environment
1. Copy `.env.example` to `.env`.
2. Fill Supabase and JWT values.
3. Add Twilio/SendGrid values for `notification-service`.

## Run
`docker compose up --build`

## Demo flow
1. Login through gateway: `POST /api/auth/login`.
2. Create prescription: `POST /api/doctor/prescriptions` (DOCTOR token).
3. Pharmacy reads incoming queue-backed prescriptions: `GET /api/pharmacy/prescriptions`.
4. Update status: `PUT /api/pharmacy/prescriptions/{id}/status`.
5. Notification Service consumes `notification_prescription_status` and sends email/SMS.
