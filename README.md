# CloneUber 🚗

API REST de una aplicación de transporte tipo Uber construida con Spring Boot.

## Tecnologías

### Backend
- **Java 25** + **Spring Boot 4**
- **Spring Security** + **JWT** para autenticación
- **Spring Data JPA** + **Hibernate** para persistencia
- **PostgreSQL** como base de datos principal
- **Docker Compose** para el entorno de desarrollo

## Requisitos previos

- JDK 21+
- Docker Desktop

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
├── rating/
│   ├── controller/     # RatingController
│   ├── service/        # RatingService
│   ├── entity/         # Rating
│   ├── dto/            # RatingRequest, RatingResponse
│   └── repository/     # RatingRepository
│
├── payment/
│   ├── controller/     # PaymentController
│   ├── service/        # PaymentService
│   ├── entity/         # Payment, PaymentStatus
│   ├── dto/            # PaymentRequest, PaymentResponse
│   └── repository/     # PaymentRepository
│
└── common/
    ├── config/         # SecurityConfig, CorsConfig, WebSocketConfig
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

### Calificaciones
| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| POST | `/api/ratings` | Calificar un viaje completado | Sí |
| GET | `/api/ratings/user/{userId}/average` | Media de puntuaciones de un usuario | Sí |

### Pagos
| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| POST | `/api/payments` | Crear un pago para un viaje completado | Sí |
| POST | `/api/payments/{id}/process` | Procesar el pago | Sí |
| GET | `/api/payments/trip/{tripId}` | Consultar el pago de un viaje | Sí |

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

## Estados de un pago

```
PENDING → COMPLETED
       → FAILED (10% de probabilidad simulada)
```

## Reglas de negocio

### Calificaciones
- Solo se pueden calificar viajes en estado `COMPLETED`
- Cada viaje solo puede tener una calificación
- Si califica el pasajero, se califica al conductor y viceversa
- La puntuación es del 1 al 5

### Pagos
- Solo se puede crear un pago si el viaje está `COMPLETED`
- El importe debe coincidir con el `finalPrice` del viaje
- Solo el pasajero del viaje puede crear y procesar su pago
- Un viaje solo puede tener un pago
- No se puede procesar un pago que ya está `COMPLETED` o `FAILED`

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
- [x] **Fase 4** — Calificaciones
- [x] **Fase 5** — Pagos simulados
- [ ] **Fase 6** — Tests unitarios con JUnti y Mockito
