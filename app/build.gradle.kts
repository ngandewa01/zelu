plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.nagesh.zelu"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nagesh.zelu"
        minSdk = 24
        targetSdk = 36
        // CI passes its run number, so every build has a higher version and installs over the last one.
        val run = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 2
        versionCode = run
        versionName = "0.3.$run"
    }

    // Release builds are signed with the upload key kept in the repository secrets (ZELU_KEYSTORE_B64,
    // ZELU_KEYSTORE_PASSWORD, ZELU_KEY_ALIAS); CI writes it to ZELU_KEYSTORE_PATH. No key is kept in the repo.
    // A local build without them signs with the debug key, which can't update an installed release.
    val releaseKey = System.getenv("ZELU_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
    signingConfigs {
        if (releaseKey != null) create("release") {
            storeFile = file(releaseKey)
            storePassword = System.getenv("ZELU_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ZELU_KEY_ALIAS")?.takeIf { it.isNotBlank() } ?: "zelu-upload"
            keyPassword = System.getenv("ZELU_KEYSTORE_PASSWORD")
        }
    }

    buildTypes {
        release {
            // Shrinking drops the unused parts of the libraries (most of the icon set). Zelu's own code is kept
            // whole (see proguard-rules.pro), so nothing in it can be renamed or removed.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // The release-only "lint vital" pass took ~5.5 of the ~8.5 CI minutes (it re-scans the large
    // MainActivity.kt). Compile errors and the parser unit tests still fail the build.
    lint {
        checkReleaseBuilds = false
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    // The activity-result code in MainActivity needs Fragment 1.3.0 or newer.
    implementation("androidx.fragment:fragment:1.8.3")
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
