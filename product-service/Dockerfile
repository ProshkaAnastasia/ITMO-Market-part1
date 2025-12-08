FROM gradle:8.14-jdk17 AS builder

WORKDIR /app

COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY src src

RUN gradle build -x test --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
