# auth-service

Schema: `auth_svc`  
Port: `8081`

## MVP scope

- Register/login/refresh/logout/me endpoints.
- Users persisted in Supabase PostgreSQL.
- BCrypt password hashing.
- JWT claims: `sub` (email), `userId`, `email`, `role`.
- Stateless sessions; JWT validated per request on protected routes.

## JWT configuration

- **Signing:** HS256 with `JWT_SECRET`. For production, use a **cryptographically random secret of at least 256 bits (32 bytes)**. The default placeholder in development is only for local use.
- **TTL:** Access and refresh lifetimes are configurable via `JWT_ACCESS_TOKEN_TTL` and `JWT_REFRESH_TOKEN_TTL` (seconds), or `jwt.access-token-ttl-seconds` / `jwt.refresh-token-ttl-seconds` in `application.yml`.

## CORS

- Allowed origins default to `http://localhost:3000`. Override with comma-separated `APP_CORS_ORIGINS` (see `.env.example`).
- Allowed headers include `Authorization` and `Content-Type` for cross-origin API calls with bearer tokens.

## Logout and token invalidation

- Logout is **stateless**: the client discards tokens. **Expired or invalid** access tokens are rejected with `401` and codes such as `TOKEN_EXPIRED` or `INVALID_TOKEN`.
- Server-side token revocation (denylist, rotation) is not implemented in this MVP.

## Demo users (seeded)

- `doctor@demo.com`
- `pharmacist@demo.com`
- `manager@demo.com`
- Seed password hash is preloaded for demo use.

## Endpoints

Invalid login credentials return **401** with error code `UNAUTHORIZED` (see `InvalidCredentialsException` handling).

| Method | Path | Auth |
|--------|------|------|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |
| POST | `/api/auth/refresh` | Public |
| POST | `/api/auth/logout` | Bearer JWT |
| GET | `/api/auth/me` | Bearer JWT |
| GET | `/api/auth/admin/ping` | Bearer JWT, role `ADMIN` only |

## RBAC matrix (auth-service)

This service enforces JWT presence and optional role checks on its own routes. Downstream microservices and the API gateway apply **additional** rules using forwarded headers (`X-User-Id`, `X-User-Role`).

| Role | Register / Login / Refresh | `/me`, `/logout` | `/admin/ping` |
|------|---------------------------|------------------|---------------|
| Unauthenticated | Yes | No (401) | No (401) |
| PATIENT | Yes | Yes | No (403) |
| DOCTOR | Yes | Yes | No (403) |
| PHARMACIST | Yes | Yes | No (403) |
| TECHNICIAN | Yes | Yes | No (403) |
| MANAGER | Yes | Yes | No (403) |
| ADMIN | Yes | Yes | Yes |

**Note:** Assignment documentation names roles such as Doctor, Pharmacist, Patient, and Admin. These map to enum values `DOCTOR`, `PHARMACIST`, `PATIENT`, and `ADMIN`. Additional roles (`TECHNICIAN`, `MANAGER`) match the broader project summary and seeded data.
