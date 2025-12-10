plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.jpa") version "1.9.25"
    id("org.springframework.boot") //version "3.2"
    id("io.spring.dependency-management") version "1.1.6"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
dependencyManagement {
    imports {
        // ✅ UPDATE THIS VERSION to 2023.0.4 to match Spring Boot 3.3.7
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.4")
    }
}
dependencies {
    // === Spring Boot основное ===
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // === Kotlin поддержка ===
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // === Spring Cloud - Config Server ===
    implementation("org.springframework.cloud:spring-cloud-config-server:4.1.0")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client:4.1.0")

    // === OpenAPI/Swagger ===
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.1.0")

    // === Development ===
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // === Тестирование ===
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}


kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
