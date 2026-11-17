plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.cheerup"
    compileSdk {
        version = release(37)
    }

    val admobAppId = providers.gradleProperty("CHEERUP_ADMOB_APP_ID")
        .getOrElse("ca-app-pub-8075634631393674~5781615068")
    val admobArcadeRewardedUnitId = providers.gradleProperty("CHEERUP_ADMOB_ARCADE_REWARDED_UNIT_ID")
        .getOrElse("ca-app-pub-8075634631393674/4055801554")
    val admobWheelsUnitId = providers.gradleProperty("CHEERUP_ADMOB_WHEELS_UNIT_ID")
        .getOrElse("ca-app-pub-8075634631393674/5905563130")
    val admobBannerUnitId = providers.gradleProperty("CHEERUP_ADMOB_BANNER_UNIT_ID")
        .getOrElse("ca-app-pub-3940256099942544/9214589741")

    defaultConfig {
        applicationId = "com.example.cheerup"
        minSdk = 33
        targetSdk = 37
        versionCode = 3
        versionName = "2.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // AdMob configuration managed via Gradle properties instead of hardcoding in Kotlin source files
        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "ADMOB_APP_ID", "\"$admobAppId\"")
        buildConfigField("String", "ADMOB_ARCADE_REWARDED_UNIT_ID", "\"$admobArcadeRewardedUnitId\"")
        buildConfigField("String", "ADMOB_WHEELS_UNIT_ID", "\"$admobWheelsUnitId\"")
        buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"$admobBannerUnitId\"")
    }

    buildTypes {
        debug {
            // Use Google's official Android test ad unit IDs automatically in development builds
            buildConfigField("boolean", "USE_TEST_ADS", "true")
        }
        release {
            buildConfigField("boolean", "USE_TEST_ADS", "false")
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    implementation(libs.play.services.games.v2)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
