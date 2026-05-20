import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

val envProperties = Properties().apply {
    // Try to load from .env first
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile.inputStream().use { this.load(it) }
    } else {
        // Fallback to local.properties if .env doesn't exist
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) {
            localFile.inputStream().use { this.load(it) }
        }
    }
}

android {
    namespace  = "com.example.madecie3"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.madecie3"
        minSdk    = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${envProperties.getProperty("GEMINI_API_KEY", "")}\""
        )
        buildConfigField(
            "String",
            "IMGBB_API_KEY",
            "\"${envProperties.getProperty("IMGBB_API_KEY", "")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        compose     = true
        buildConfig = true
        // NOTE: No composeOptions block needed — the kotlin.compose plugin
        // (alias above) manages the Compose compiler version automatically
        // for Kotlin 2.0+. Adding composeOptions here would conflict.
    }
}

dependencies {

    // ── Compose (versions come from BOM 2025.04.01 — compatible with Kotlin 2.0.21) ──
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // ── Firebase (BOM handles all versions) ──────────────────────────────────
    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // ── Image loading ─────────────────────────────────────────────────────────
    implementation("com.github.bumptech.glide:glide:4.16.0")
    // No kapt/ksp needed unless you define a custom @GlideModule

    // ── ML Kit OCR (timetable scanning) ──────────────────────────────────────
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // ── Coroutines (required by TimetableScanner.await()) ────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // ── AndroidX / Material ───────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.circleimageview)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.activity:activity-ktx:1.10.1")

    // ── Tests ─────────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
