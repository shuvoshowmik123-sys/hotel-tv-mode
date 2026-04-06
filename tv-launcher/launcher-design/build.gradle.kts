plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

providers.gradleProperty("customBuildRoot").orNull?.let { customBuildRoot ->
    layout.buildDirectory.set(file("$customBuildRoot/launcher-design"))
}

android {
    namespace = "com.hotelvision.launcher.design"
    compileSdk = 35

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = false
    }
}

dependencies {
    implementation(libs.androidx.core.splashscreen)
}
