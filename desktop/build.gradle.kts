plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material)
    implementation(project(":core:sync"))
    implementation(project(":core:truth"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}

compose.desktop {
    application {
        mainClass = "com.flow.pharos.desktop.MainKt"
        nativeDistributions {
            packageName = "Pharos"
            packageVersion = "1.0.0"
        }
    }
}
