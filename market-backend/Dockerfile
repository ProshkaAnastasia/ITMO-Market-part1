FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Copy gradle files
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./

# Copy source code
COPY src src

# Build the application (without running tests)
RUN gradle build -x test --no-daemon

# Stage 2: Runtime
# ✅ ИСПРАВЛЕНО: Используйте eclipse-temurin:17-jre вместо alpine версии
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
