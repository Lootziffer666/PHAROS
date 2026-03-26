import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) localProps.load(localPropsFile.inputStream())

fun localProp(key: String, default: String = "") =
    localProps.getProperty(key, default).ifBlank { default }

android {
    namespace = "com.flow.pharos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.flow.pharos"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "PERPLEXITY_API_KEY",
            "\"${localProp("PERPLEXITY_API_KEY")}\"")
        buildConfigField("String", "OLLAMA_BASE_URL",
            "\"${localProp("OLLAMA_BASE_URL", "http://10.0.2.2:11434")}\"")
        buildConfigField("String", "CUSTOM_OPENAI_BASE_URL",
            "\"${localProp("CUSTOM_OPENAI_BASE_URL", "http://10.0.2.2:8080/v1/")}\"")
        buildConfigField("String", "CUSTOM_OPENAI_API_KEY",
            "\"${localProp("CUSTOM_OPENAI_API_KEY", "local")}\"")
        buildConfigField("String", "CHAT_ID", "\"CH-20260308-04\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:storage"))
    implementation(project(":core:sync"))
    implementation(project(":core:truth"))
    implementation(project(":core:llm"))
    implementation(project(":feature:archive"))
    implementation(project(":feature:relations"))
    implementation(project(":feature:settings"))
    implementation(project(":provider:perplexity"))
    implementation(project(":provider:ollama"))
    implementation(project(":provider:customopenai"))

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Activity & Lifecycle
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-android-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // PDFBox
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // DocumentFile
    implementation("androidx.documentfile:documentfile:1.0.1")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Material (View-based) — required for Theme.Material3.DayNight.NoActionBar in AndroidManifest
    implementation("com.google.android.material:material:1.12.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
}
