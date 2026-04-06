plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

providers.gradleProperty("customBuildDir").orNull?.let { customBuildDir ->
    layout.buildDirectory.set(file(customBuildDir))
}
providers.gradleProperty("customBuildRoot").orNull?.let { customBuildRoot ->
    layout.buildDirectory.set(file("$customBuildRoot/app"))
}

android {
    namespace = "com.hotelvision.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hotelvision.launcher"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            // Temporary fallback while R8 crashes with ConcurrentModificationException
            isMinifyEnabled = false
            isShrinkResources = false
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
        compose = true
    }
    sourceSets.named("main") {
        java.exclude(
            "com/hotelvision/launcher/data/**",
            "com/hotelvision/launcher/di/**",
            "com/hotelvision/launcher/performance/**",
            "com/hotelvision/launcher/workers/**",
            "com/hotelvision/launcher/setup/DefaultLauncherCoordinator.kt",
            "com/hotelvision/launcher/setup/DefaultLauncherPromptStore.kt",
            "com/hotelvision/launcher/ui/InstalledAppMappers.kt",
            "com/hotelvision/launcher/ui/LauncherModels.kt",
            "com/hotelvision/launcher/ui/LauncherViewModel.kt",
            "com/hotelvision/launcher/ui/MockHotelContent.kt"
        )
        res.srcDirs("../launcher-design/src/main/res")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":launcher-core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    
    // TV
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    // Coil
    implementation(libs.coil)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Palette API
    implementation("androidx.palette:palette:1.0.0")

    // Lottie & Utils
    implementation(libs.lottie.compose)
    implementation(libs.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Extra
    implementation(libs.androidx.metrics.performance)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.tvprovider)
    implementation(libs.androidx.profileinstaller)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso.core)
}
