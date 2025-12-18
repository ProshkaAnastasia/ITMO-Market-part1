dependencies {
    // === Spring Cloud Gateway ===
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    implementation("org.springframework.cloud:spring-cloud-starter-config")

    // Swagger / OpenAPI
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:${rootProject.extra["springDocVersion"]}")
}