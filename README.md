# CloneUber 🚗

API REST de una aplicación de transporte tipo Uber construida con Spring Boot.

## Tecnologías

- **Java 25** + **Spring Boot 4**
- **Spring Security** + **JWT** para autenticación
- **Spring Data JPA** + **Hibernate** para persistencia
- **PostgreSQL** como base de datos principal
- **Redis** para tracking en tiempo real (Fase 4)
- **Docker Compose** para el entorno de desarrollo

## Requisitos previos

- JDK 21+
- Docker Desktop
- IntelliJ IDEA (recomendado)

## Arrancar el proyecto

**1. Levantar la base de datos:**
```bash
docker compose up -d
```

**2. Arrancar Spring Boot:**

Desde IntelliJ: botón de play sobre `CloneUberApplication.java`

O desde terminal:
```bash
./mvnw spring-boot:run
```

La app arranca en `http://localhost:8080`

## Estructura del proyecto

```
src/main/java/com/devmark/cloneuber/
│
├── auth/
│   ├── controller/     # AuthController (register, login)
│   ├── service/        # AuthService
│   ├── dto/            # RegisterRequest, LoginRequest, AuthResponse
│   └── security/       # JwtService, JwtAuthFilter
│
├── user/
│   ├── controller/
│   ├── entity/         # User, Role
│   └── repository/     # UserRepository
│
├── driver/
│   ├── controller/     # DriverController
│   ├── entity/         # DriverProfile
│   └── repository/     # DriverProfileRepository
│
├── trip/
│   ├── controller/     # TripController
│   ├── service/        # TripService
│   ├── entity/         # Trip, TripStatus
│   ├── dto/            # TripRequest, TripResponse
│   └── repository/     # TripRepository
│
└── common/
    ├── config/         # SecurityConfig
    ├── exception/
    └── response/       # ApiResponse
```

## Endpoints disponibles

### Autenticación
| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| POST | `/api/auth/register` | Registro de usuario | No |
| POST | `/api/auth/login` | Login | No |

### Conductores
| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| POST | `/api/drivers/profile` | Crear perfil de conductor | Sí |
| PATCH | `/api/drivers/availability` | Actualizar disponibilidad y ubicación | Sí |

### Viajes
| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| POST | `/api/trips` | Solicitar un viaje | Sí |
| PATCH | `/api/trips/{id}/status` | Cambiar estado del viaje | Sí |
| GET | `/api/trips/my` | Ver mis viajes | Sí |

## Autenticación

Todas las rutas protegidas requieren un token JWT en la cabecera:

```
Authorization: Bearer <token>
```

El token se obtiene en el registro o login.

## Estados de un viaje

```
REQUESTED → ACCEPTED → IN_PROGRESS → COMPLETED
                ↓              ↓
            CANCELLED      CANCELLED
```

## Variables de configuración

```yaml
jwt:
  secret: tu-clave-secreta-de-32-caracteres
  expiration: 86400000  # 24 horas en ms

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/uberdb
    username: postgres
    password: postgres
```

## Fases del proyecto

- [x] **Fase 1** — Setup del proyecto
- [x] **Fase 2** — Autenticación JWT
- [x] **Fase 3** — Gestión de viajes
- [ ] **Fase 4** — Tracking en tiempo real (WebSockets + Redis)
- [ ] **Fase 5** — Pagos y calificaciones