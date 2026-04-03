# gateway-service

Schema: `none`  
Port: `8080`

## Purpose
- Single API entry point for frontend traffic.
- Validates JWT on protected `/api/**` routes.
- Injects `X-User-Id` and `X-User-Role` headers for downstream services.

## Public routes
- `/api/auth/login`
- `/api/auth/register`
- `/api/auth/refresh`

## Routed backends
- `/api/auth/**` -> `auth-service`
- `/api/doctor/**` -> `prescription-service`
- `/api/pharmacy/**` -> `pharmacy-service`
