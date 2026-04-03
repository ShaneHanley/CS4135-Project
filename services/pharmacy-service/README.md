# pharmacy-service

Schema: `pharmacy_svc`  
Port: `8083`

## MVP scope
- Consumes new prescriptions from pgmq queue `prescription_created`.
- Stores queue payload in Supabase schema `pharmacy_svc`.
- Allows pharmacist/manager status updates.
- Publishes notification payload to `notification_prescription_status`.

## Endpoints
- `GET /api/pharmacy/prescriptions`
- `GET /api/pharmacy/prescriptions/{id}`
- `PUT /api/pharmacy/prescriptions/{id}/status`
- `GET /api/pharmacy/dashboard/stats`

## Notification payload published (exact)
- `prescriptionId`
- `patientId`
- `patientEmail`
- `patientName`
- `newStatus`
- `rejectionReason`
- `pharmacyName`
