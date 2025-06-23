import com.android.tools.build.jetifier.core.utils.Log
import java.util.Properties

import kotlin.system.exitProcess

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    kotlin("plugin.serialization") version "1.9.20"
}

android {
    namespace = "project.unibo.tankyou"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "project.unibo.tankyou"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        val p = Properties()
        val f = rootProject.file("local.properties")

        var url: String
        var key: String

        if (f.exists()) {
            p.load(f.inputStream())

            try {
                url = p.getProperty("database.url")
            } catch (e: Exception) {
                Log.e(
                    "[BUILD]",
                    "Database URL Not Found in 'local.properties'! Exiting Process...\nError Tracestack:",
                    e
                )
                exitProcess(-1)
            }

            try {
                key = p.getProperty("database.key")
            } catch (e: Exception) {
                Log.e(
                    "[BUILD]",
                    "Database KEY Not Found in 'local.properties'! Exiting Process...\nError Tracestack:",
                    e
                )
                exitProcess(-1)
            }
        } else {
            Log.e(
                "[BUILD]",
                "File 'local.properties' Not Found! Exiting Process..."
            )
            exitProcess(-1)
        }

        debug {
            buildConfigField("String", "DATABASE_URL", "\"${url}\"")
            buildConfigField("String", "DATABASE_KEY", "\"${key}\"")
            buildConfigField(
                "String[]",
                "AUTHORS",
                "{ \"Marco Marrelli\", \"Margherita Zanchini\" }"
            )
        }
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            buildConfigField("String", "DATABASE_URL", "\"${url}\"")
            buildConfigField("String", "DATABASE_KEY", "\"${key}\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.recyclerview)

    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.material)

    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.androidx.preference.ktx)

    implementation(libs.osmdroid.android)
    implementation("com.github.MKergall:osmbonuspack:6.8.0")

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    implementation(libs.opencsv)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.okhttp)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.postgrest.kt.v260)
    implementation(libs.realtime.kt.v260)
    implementation(libs.storage.kt)
    implementation(libs.gotrue.kt)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.kotlinx.datetime)

    implementation(libs.coil.compose)
}