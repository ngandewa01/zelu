plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.nagesh.zelu"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nagesh.zelu"
        minSdk = 24
        targetSdk = 35
        // CI passes its run number, so every build has a higher version and installs over the last one.
        val run = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 2
        versionCode = run
        versionName = "0.3.$run"
    }

    signingConfigs {
        create("release") {
            // The signing key must stay the same for every build, so the app updates in place and keeps its data.
            // CI supplies it from secrets (ZELU_KEYSTORE_PATH, ZELU_KEYSTORE_PASSWORD, ZELU_KEY_ALIAS); without them
            // the key committed beside this file is used, so a local build still works.
            val envPath = System.getenv("ZELU_KEYSTORE_PATH")
            val envPass = System.getenv("ZELU_KEYSTORE_PASSWORD")
            storeFile = file(if (!envPath.isNullOrBlank()) envPath else "zelu-release.jks")
            storePassword = if (!envPass.isNullOrBlank()) envPass else "zelupass"
            keyAlias = System.getenv("ZELU_KEY_ALIAS")?.takeIf { it.isNotBlank() } ?: "zelu"
            keyPassword = if (!envPass.isNullOrBlank()) envPass else "zelupass"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
}
