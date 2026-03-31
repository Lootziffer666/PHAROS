plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
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
