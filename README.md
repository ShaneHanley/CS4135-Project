# Prescription Management System

A microservices-based web application enabling doctors to send prescriptions to pharmacies electronically. Doctors can log in, select a pharmacy, and submit patient prescriptions. Pharmacies receive prescriptions in real-time, update their status, and the notification service automatically alerts patients via SMS or email about their prescription progress.

**Links:**
- [Microsoft Planner](https://planner.cloud.microsoft/webui/v1/plan/GwUPy9Dg-E-eI9f3jqD2z5cAHaZW?tid=0084b924-3ab4-4116-9251-9939f695e54c)
- [Project Wiki](https://github.com/ShaneHanley/CS4135-Project/wiki)

---

## Architecture Overview

```
web-frontend (React + Vite)
      |
      v
gateway-service :8080       ← JWT validation, header injection, routing
      |
      +---> auth-service          :8081  (register/login/refresh/logout)
      +---> prescription-service  :8082  (doctor creates prescriptions)
      +---> pharmacy-service      :8083  (pharmacist manages prescriptions)
      +---> patient-service       :8084
      +---> inventory-service     :8085
      +---> billing-service       :8086
      +---> analytics-service     :8087
      +---> notification-service  :8088  (SMS via Twilio, email via SendGrid)
```

**Async messaging:** `prescription-service` publishes to a `prescription_created` PGMQ queue. `pharmacy-service` consumes from it and publishes status updates to `notification_prescription_status`. `notification-service` polls that queue and dispatches SMS/email to patients.

**Database:** Supabase PostgreSQL — each service uses its own schema (schema-per-service isolation).

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18, Vite, Redux Toolkit, React Router, Recharts |
| Backend | Spring Boot 3.3 (Java 17), Spring Cloud 2023 |
| Database | Supabase PostgreSQL (Flyway migrations) |
| Messaging | PGMQ (Postgres-based message queue) |
| Auth | JWT (EC key pair), BCrypt |
| Notifications | Twilio (SMS), SendGrid (email) |
| Container | Docker / Docker Compose |

---

## Project Structure

```
.
├── common-libs/
│   └── common-messaging/       # Shared PGMQ messaging library
├── infrastructure/
│   ├── auth-service/           # Auth, JWT, user management  (:8081)
│   └── gateway-service/        # API gateway, JWT filter      (:8080)
├── services/
│   ├── prescription-service/   # Doctor prescription creation  (:8082)
│   ├── pharmacy-service/       # Pharmacy prescription mgmt   (:8083)
│   ├── patient-service/        # Patient data                  (:8084)
│   ├── inventory-service/      # Pharmacy inventory            (:8085)
│   ├── billing-service/        # Billing                       (:8086)
│   ├── analytics-service/      # Analytics/reporting           (:8087)
│   └── notification-service/   # SMS + email notifications     (:8088)
└── web-frontend/               # React SPA                     (:3000)
```

---

## Getting Started

### Prerequisites

- Docker and Docker Compose
- A Supabase project (for PostgreSQL)
- Twilio account (SMS notifications)
- SendGrid account (email notifications)

### 1. Configure environment variables

Copy `.env.example` to `.env` and fill in your values:

```bash
cp .env.example .env
```

| Variable | Description |
|---|---|
| `SUPABASE_DB_URL` | JDBC URL for Supabase PostgreSQL |
| `SUPABASE_MIGRATION_URL` | Migration-specific DB URL |
| `SUPABASE_DB_USER` | Database username |
| `SUPABASE_DB_PASSWORD` | Database password |
| `SUPABASE_URL` | Supabase project URL |
| `SUPABASE_SERVICE_KEY` | Supabase service role key |
| `JWT_EC_PRIVATE_KEY_B64` | Base64-encoded EC private key |
| `JWT_EC_PUBLIC_KEY_B64` | Base64-encoded EC public key |
| `VITE_API_BASE_URL` | Frontend API base URL (default: `http://localhost:8080`) |
| `TWILIO_ACCOUNT_SID` | Twilio account SID |
| `TWILIO_AUTH_TOKEN` | Twilio auth token |
| `TWILIO_PHONE_NUMBER` | Twilio sender phone number |
| `SENDGRID_API_KEY` | SendGrid API key |
| `SENDGRID_FROM_EMAIL` | SendGrid sender email address |
| `AUTH_SERVICE_URL` | Auth service URL (default: `http://localhost:8081`) |
| `PRESCRIPTION_SERVICE_URL` | Prescription service URL (default: `http://localhost:8082`) |
| `PHARMACY_SERVICE_URL` | Pharmacy service URL (default: `http://localhost:8083`) |
| `PATIENT_SERVICE_URL` | Patient service URL (default: `http://localhost:8084`) |
| `INVENTORY_SERVICE_URL` | Inventory service URL (default: `http://localhost:8085`) |
| `BILLING_SERVICE_URL` | Billing service URL (default: `http://localhost:8086`) |
| `ANALYTICS_SERVICE_URL` | Analytics service URL (default: `http://localhost:8087`) |

### 2. Start all services

```bash
docker compose up --build
```

The frontend will be available at `http://localhost:3000`.

### 3. Demo users (pre-seeded)

| Email | Role |
|---|---|
| `doctor@demo.com` | DOCTOR |
| `pharmacist@demo.com` | PHARMACIST |
| `manager@demo.com` | MANAGER |

---

## Service Endpoints

### Auth Service (`:8081`)

| Method | Path | Auth |
|---|---|---|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |
| POST | `/api/auth/refresh` | Public |
| POST | `/api/auth/logout` | Bearer JWT |
| GET | `/api/auth/me` | Bearer JWT |

### Prescription Service (`:8082`)

| Method | Path | Auth |
|---|---|---|
| GET | `/api/doctor/prescriptions` | Bearer JWT (DOCTOR) |
| POST | `/api/doctor/prescriptions` | Bearer JWT (DOCTOR) |
| GET | `/api/doctor/prescriptions/{id}/status` | Bearer JWT (DOCTOR) |
| GET | `/api/doctor/pharmacies` | Bearer JWT (DOCTOR) |

### Pharmacy Service (`:8083`)

| Method | Path | Auth |
|---|---|---|
| GET | `/api/pharmacy/prescriptions` | Bearer JWT (PHARMACIST/MANAGER) |
| GET | `/api/pharmacy/prescriptions/{id}` | Bearer JWT (PHARMACIST/MANAGER) |
| PUT | `/api/pharmacy/prescriptions/{id}/status` | Bearer JWT (PHARMACIST/MANAGER) |
| GET | `/api/pharmacy/dashboard/stats` | Bearer JWT (PHARMACIST/MANAGER) |

All requests go through the gateway at `:8080`. The gateway validates JWTs and injects `X-User-Id` and `X-User-Role` headers for downstream services.

---

## Async Message Flow

```
prescription-service
  --[prescription_created]--> pharmacy-service
                                  --[notification_prescription_status]--> notification-service
                                                                              --> Twilio SMS
                                                                              --> SendGrid Email
```

`prescription_created` payload: `prescriptionId`, `doctorId`, `patientId`, `patientEmail`, `patientName`, `pharmacyId`, `medicationName`, `dosage`, `quantity`, `createdAt`

`notification_prescription_status` payload: `prescriptionId`, `patientId`, `patientEmail`, `patientName`, `newStatus`, `rejectionReason`, `pharmacyName`