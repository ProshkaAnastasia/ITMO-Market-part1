# Stage 1: Gradle cache builder (только dependencies)
FROM gradle:8.14-jdk17 AS dependency-resolver
WORKDIR /app

# Копируем ТОЛЬКО gradle-файлы (минимум, кэшируется)
COPY gradle/wrapper gradle/wrapper
COPY gradle.properties settings.gradle.kts build.gradle.kts ./
COPY eureka-server/build.gradle.kts eureka-server/
COPY config-server/build.gradle.kts config-server/
COPY api-gateway/build.gradle.kts api-gateway/
COPY user-service/build.gradle.kts user-service/
COPY product-service/build.gradle.kts product-service/
COPY order-service/build.gradle.kts order-service/

# Скачиваем зависимости (кэшируется на слое)
RUN gradle :eureka-server:dependencies --no-daemon 2>&1 | grep -E "^|---|.*" || true

# Stage 2: Builder (только eureka-server)
FROM dependency-resolver AS eureka-builder

# Копируем исходники eureka-server
COPY eureka-server/src eureka-server/src

# Собираем только eureka-server
RUN gradle :eureka-server:build -x test --no-daemon -q

# Stage 3: Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app

# Копируем jar из builder'а
COPY --from=eureka-builder /app/eureka-server/build/libs/*.jar app.jar

# Создаем непривилегированного пользователя
RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser

EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
