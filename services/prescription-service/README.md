# prescription-service

Schema: `prescription_svc`  
Port: `8082`

## MVP scope
- Doctor creates and tracks prescriptions.
- Prescriptions persisted in Supabase schema `prescription_svc`.
- On create, publishes event to pgmq queue `prescription_created`.

## Endpoints
- `GET /api/doctor/prescriptions`
- `POST /api/doctor/prescriptions`
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
