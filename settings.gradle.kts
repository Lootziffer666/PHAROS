pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "Pharos"
include(
    ":app",
    ":core:model",
    ":core:storage",
    ":core:sync",
    ":core:truth",
    ":core:llm",
    ":provider:perplexity",
    ":provider:ollama",
    ":provider:customopenai",
    ":desktop",
)
