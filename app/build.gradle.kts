import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kover)
}

android {
    namespace = "com.duoc.huertify_app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.duoc.huertify_app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "WEATHER_BASE_URL", "\"https://api.open-meteo.com/\"")
        buildConfigField("String", "HUERTIFY_BASE_URL", "\"http://172.23.96.1:8080/\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Firma del APK de release. Los datos del .jks se leen de keystore.properties (raíz del
    // proyecto, fuera de Git). Si el archivo no existe, el release se genera sin firmar.
    val keystoreFile = rootProject.file("keystore.properties")
    val keystoreProps = Properties().apply {
        if (keystoreFile.exists()) keystoreFile.inputStream().use { load(it) }
    }
    signingConfigs {
        if (keystoreFile.exists()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation(libs.androidx.sqlite.framework)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
}

kover {
    reports {
        filters {
            includes {
                packages(
                    "com.duoc.huertify_app.domain",
                    "com.duoc.huertify_app.domain.validation",
                    "com.duoc.huertify_app.data.repository"
                )
            }
        }
    }
}

val app = "Huertify"
val version = android.defaultConfig.versionName ?: "1.0"
val build = android.defaultConfig.versionCode ?: 1

tasks.register<Copy>("exportarApkRelease") {
    dependsOn("assembleRelease")
    val app = "Huertify"
    val version = android.defaultConfig.versionName ?: "1.0"
    val build = android.defaultConfig.versionCode ?: 1
    val nombreApk = "$app-v$version-build$build.apk"

    from(layout.buildDirectory.dir("outputs/apk/release")) {
        include("app-release.apk")
        rename { nombreApk }
    }
    into(layout.buildDirectory.dir("apk-exportadas"))
}