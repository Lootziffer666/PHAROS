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
