import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.ksp.plugin)
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashlytics)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.roborazzi.plugin)
}
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) localProperties.load(FileInputStream(localPropertiesFile))

android {
    namespace = "com.weathersync"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.weathersync"
        minSdk = 28
        targetSdk = 34
        versionCode = 17
        versionName = "1.0.2-alpha"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField("String", "WEB_CLIENT_ID", "\"${localProperties.getProperty("WEB_CLIENT_ID")}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties.getProperty("GEMINI_API_KEY")}\"")
        buildConfigField("String", "OPENAI_API_KEY", "\"${localProperties.getProperty("OPENAI_API_KEY")}\"")
        buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"${localProperties.getProperty("INTERSTITIAL_AD_UNIT_ID")}\"")
        buildConfigField("String", "HOME_PROMO_AD_UNIT_ID", "\"${localProperties.getProperty("HOME_PROMO_AD_UNIT_ID")}\"")
        buildConfigField("String", "ACTIVITY_PLANNING_PROMO_AD_UNIT_ID", "\"${localProperties.getProperty("ACTIVITY_PLANNING_PROMO_AD_UNIT_ID")}\"")
    }
    buildFeatures.buildConfig = true
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    buildTypes {
        release {
            isDebuggable = false
            configure<CrashlyticsExtension> {
                nativeSymbolUploadEnabled = true
                unstrippedNativeLibsDir = file("${project.buildDir}/intermediates/merged_native_libs/release/mergeReleaseNativeLibs/out/lib")
            }
            ndk.debugSymbolLevel = "FULL"
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
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
    implementation(libs.androidx.material.icons.extended)

    // Navigation
    implementation(libs.navigation.compose)

    implementation(libs.kotlinx.serialization.json)

    // Google auth
    implementation(libs.play.services.auth)

    // firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.crashlytics.ndk)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // location
    implementation(libs.play.services.location)

    // permissions
    implementation(libs.accompanist.permissions)

    // ktor
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.json)
    implementation(libs.ktor.client.serialization)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.mock)
    implementation(libs.ktor.client.content.negotiation)

    // room
    implementation(libs.room.runtime)
    implementation(libs.androidx.espresso.intents)
    annotationProcessor(libs.room.compiler)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)
    implementation(libs.gson)

    // datastore
    implementation(libs.datastore.preferences)

    // DI
    implementation(libs.koin)

    // AI
    implementation(libs.generative.ai)
    implementation(libs.openai.client)

    implementation(libs.splashscreen)
    implementation(libs.play.integrity)
    implementation(libs.appcheck.debug)

    // Billing
    implementation(libs.billing.client)

    // Ads
    implementation(libs.play.services.ads)

    // In-app review
    implementation(libs.play.review)
    implementation(libs.play.review.ktx)

    implementation(libs.kotlin.test.junit)
    implementation(libs.androidx.junit.ktx)
    testImplementation(libs.room.testing)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockk.agent)
    testImplementation(libs.mockk.android)
    testImplementation(libs.ui.test.junit)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    // A small testing library for kotlinx.coroutines Flow
    testImplementation(libs.turbine)
    testImplementation(libs.navigation.testing)


    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.ui.test.manifest)
}