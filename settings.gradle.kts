pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // Garante que apenas repositórios definidos aqui sejam usados
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "VictorAvancada"
include(":app")
include(":RaceLibrary")
