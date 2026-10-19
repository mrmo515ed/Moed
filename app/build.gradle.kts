import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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
        versionCode = 7
        versionName = "0.3.4"

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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.coil.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
}
