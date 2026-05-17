# ── Stage 1: Build ───────────────────────────────────────────────────────────
# Usa Maven con Java 21 para compilar el JAR
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copia primero el pom.xml para aprovechar la caché de Docker.
# Si el código cambia pero no las dependencias, Maven no las re-descarga.
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

RUN ./mvnw dependency:go-offline -q

# Ahora copia el código fuente y compila
COPY src ./src
RUN ./mvnw package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
# Imagen final mucho más ligera: solo JRE, sin Maven ni código fuente
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Usuario sin privilegios (buena práctica de seguridad)
RUN addgroup -S cloneuber && adduser -S cloneuber -G cloneuber
USER cloneuber

# Copia solo el JAR del stage anterior
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
