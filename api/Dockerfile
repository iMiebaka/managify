# syntax=docker/dockerfile:1
# Two-stage build:
#   Stage 1 (builder): Full JDK + Maven — compiles the JAR
#   Stage 2 (runtime): Minimal JRE only — smaller image, minimal attack surface

FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src ./src

# --mount=type=cache persists the Maven local repo across builds.
RUN --mount=type=cache,target=/root/.m2 \
    chmod +x mvnw && \
    ./mvnw package -DskipTests -B --no-transfer-progress

# ── Runtime ───────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S managify && adduser -S managify -G managify

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar
RUN chown managify:managify app.jar

USER managify

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
