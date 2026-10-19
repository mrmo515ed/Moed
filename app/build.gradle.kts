import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun readLocalOrEnvironment(propertyName: String, environmentName: String): String {
    val properties = Properties()
    val localProperties = rootProject.file("local.properties")
    if (localProperties.exists()) {
        localProperties.inputStream().use(properties::load)
    }
    val fromEnvironment = providers.environmentVariable(environmentName).orNull?.takeIf { it.isNotBlank() }
    val fromLocalProperties = properties.getProperty(propertyName)?.takeIf { it.isNotBlank() }
    return fromEnvironment ?: fromLocalProperties ?: when (propertyName) {
        "SUPABASE_URL" -> "https://kpowibropzeapdfpkhqo.supabase.co"
        // A Supabase publishable key is designed to be embedded in untrusted clients.
        "SUPABASE_PUBLISHABLE_KEY" -> "sb_publishable_1R1B0_4SJPO1vPWA8lkV-g_LAbUnpf9"
        else -> ""
    }
}

fun asBuildConfigString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val supabaseUrl = readLocalOrEnvironment("SUPABASE_URL", "SUPABASE_URL")
val supabasePublishableKey = readLocalOrEnvironment("SUPABASE_PUBLISHABLE_KEY", "SUPABASE_PUBLISHABLE_KEY")

android {
    namespace = "com.animeblack.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.animeblack.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "0.3.1"

        buildConfigField("String", "SUPABASE_URL", asBuildConfigString(supabaseUrl))
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", asBuildConfigString(supabasePublishableKey))
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    val composeBom = platform("androidx.compose:compose-bom:2025.03.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.media3:media3-exoplayer:1.6.1")
    implementation("androidx.media3:media3-ui:1.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
