import java.util.Properties

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
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        val p = Properties()
        val f = rootProject.file("local.properties")

        var url: String = ""
        var key: String = ""

        if (f.exists()) {
            p.load(f.inputStream())

            url = p.getProperty("database.url")
            key = p.getProperty("database.key")
        }

        debug {
            buildConfigField("String", "DATABASE_URL", "\"${url}\"")
            buildConfigField("String", "DATABASE_KEY", "\"${key}\"")
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
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)

    // @DOC: Libreria per Gestione delle Preferenze (PreferenceManager)
    implementation(libs.androidx.preference.ktx)

    // @DOC: Libreria 'osmdroid' per Mappe OpenStreetMap (OSM)
    implementation(libs.osmdroid.android)

    // @DOC: (Opzionale) Funzionalità Aggiuntive (Ricerca di Indirizzi...)
    implementation(libs.osmdroid.mapsforge)

    // @DOC: Libreria Room per Database Interno
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // @DOC: Libreria per Lettura/Fetching di File .csv
    implementation(libs.opencsv)

    // @DOC: Libreria per Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // @DOC: Librerie per Download dei File .csv
    implementation(libs.okhttp)
    implementation(libs.androidx.work.runtime.ktx)

    // @DOC: Librerie per Database Esterno (Supabase)
    implementation(libs.postgrest.kt.v260)
    implementation(libs.realtime.kt.v260)
    implementation(libs.storage.kt)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // DateTime
    implementation(libs.kotlinx.datetime)
}