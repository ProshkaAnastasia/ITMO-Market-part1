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

RUN gradle :api-gateway:dependencies --no-daemon 2>&1 | grep -E "^|---|.*" || true

# Stage 2: Builder (только api-gateway)
FROM dependency-resolver AS api-gateway-builder
COPY api-gateway/src api-gateway/src
RUN gradle :api-gateway:build -x test --no-daemon -q

# Stage 3: Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=api-gateway-builder /app/api-gateway/build/libs/*.jar app.jar
RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
