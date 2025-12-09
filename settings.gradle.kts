pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "itmo-market"

include(
    ":user-service",
    ":product-service",
    ":order-service",
    ":eureka-server",
    ":api-gateway",
    ":config-server"
)
