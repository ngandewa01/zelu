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
            // A fixed, committed key so every build is signed identically. This lets the
            // app update in place instead of being an uninstall+reinstall, so saved data
            // (classifications, payee rules, transactions) survives upgrades.
            storeFile = file("zelu-release.jks")
            storePassword = "zelupass"
            keyAlias = "zelu"
            keyPassword = "zelupass"
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
