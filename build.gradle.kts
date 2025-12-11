plugins {
    id("org.springframework.boot") version "3.4.0" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
    kotlin("jvm") version "1.9.25" apply false
    kotlin("plugin.spring") version "1.9.25" apply false
    kotlin("plugin.jpa") version "1.9.25" apply false
}

// ==================== VERSION VARIABLES ====================
val springCloudVersion = "2024.0.0"
val springDocVersion = "2.7.0"
val testcontainersVersion = "1.19.3"
val mockitoKotlinVersion = "5.1.0"
val mockitoCoreVersion = "5.2.0"
val javaVersion = "17"
val postgresqlVersion = "15"

allprojects {
    group = "ru.itmo"
    version = "0.0.1-SNAPSHOT"
}

subprojects {

    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")

    // ==================== SPRING CLOUD DEPENDENCY MANAGEMENT ====================
    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
        }
    }

    // ==================== DEPENDENCY MANAGEMENT ====================
    dependencies {
        // Spring Boot Basic (Actuator, Validation)
        add("implementation", "org.springframework.boot:spring-boot-starter-actuator")
        add("implementation", "org.springframework.boot:spring-boot-starter-validation")

        // Kotlin - ВСЕМ сервисам
        add("implementation", "org.jetbrains.kotlin:kotlin-reflect")
        add("implementation", "org.jetbrains.kotlin:kotlin-stdlib")

        // Jackson - ВСЕМ сервисам
        add("implementation", "com.fasterxml.jackson.module:jackson-module-kotlin")

        // Swagger / OpenAPI сервисам
        add("implementation", "org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")

        // DevTools - для удобства локальной разработки
        add("developmentOnly", "org.springframework.boot:spring-boot-devtools")

        // ==================== ТЕСТЫ ====================
        add("testImplementation", "org.springframework.boot:spring-boot-starter-test")
        add("testImplementation", "org.testcontainers:testcontainers:$testcontainersVersion")
        add("testImplementation", "org.testcontainers:junit-jupiter:$testcontainersVersion")
        add("testImplementation", "org.testcontainers:postgresql:$testcontainersVersion")
        add("testImplementation", "org.mockito.kotlin:mockito-kotlin:$mockitoKotlinVersion")
        add("testImplementation", "org.mockito:mockito-core:$mockitoCoreVersion")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }

    // ==================== JAVA TOOLCHAIN ====================
    extensions.getByType<JavaPluginExtension>().apply {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(javaVersion.toInt()))
        }
    }

    // ==================== KOTLIN COMPILER ====================
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    // ==================== JPA ANNOTATIONS ====================
    configure<org.jetbrains.kotlin.allopen.gradle.AllOpenExtension> {
        annotation("jakarta.persistence.Entity")
        annotation("jakarta.persistence.MappedSuperclass")
        annotation("jakarta.persistence.Embeddable")
    }

    // ==================== TESTS ====================
    tasks.withType<Test> {
        useJUnitPlatform()
    }
}