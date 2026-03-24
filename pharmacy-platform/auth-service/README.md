# auth-service

Schema: `auth_svc`  
Port: `8081`

## MVP scope
- Register/login/refresh/logout/me endpoints.
- Users persisted in Supabase PostgreSQL.
- BCrypt password hashing.
- JWT claims: `userId`, `email`, `role`.

## Demo users (seeded)
- `doctor@demo.com`
- `pharmacist@demo.com`
- `manager@demo.com`
- Seed password hash is preloaded for demo use.

## Endpoints
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/auth/me`
