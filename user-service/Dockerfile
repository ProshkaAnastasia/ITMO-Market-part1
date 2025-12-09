# Stage 1: Gradle cache builder (только dependencies)
FROM gradle:8.14-jdk17 AS dependency-resolver
WORKDIR /app

COPY gradle/wrapper gradle/wrapper
COPY gradle.properties settings.gradle.kts build.gradle.kts ./
COPY eureka-server/build.gradle.kts eureka-server/
COPY config-server/build.gradle.kts config-server/
COPY api-gateway/build.gradle.kts api-gateway/
COPY user-service/build.gradle.kts user-service/
COPY product-service/build.gradle.kts product-service/
COPY order-service/build.gradle.kts order-service/

RUN gradle :user-service:dependencies --no-daemon 2>&1 | grep -E "^|---|.*" || true

# Stage 2: Builder (только user-service)
FROM dependency-resolver AS user-builder
COPY user-service/src user-service/src
RUN gradle :user-service:build -x test --no-daemon -q

# Stage 3: Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=user-builder /app/user-service/build/libs/*.jar app.jar
RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
