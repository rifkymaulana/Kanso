pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Kanso"

include(":app")
include(":core:model")
include(":core:database")
include(":core:security")
include(":core:backup")
include(":core:ui")
include(":feature:today")
include(":feature:money")
include(":feature:brain")
include(":feature:habits")
include(":feature:lifeadmin")
include(":feature:utilities")
