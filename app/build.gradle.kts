plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.koolicream.zelu"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.koolicream.zelu"
        minSdk = 24
        targetSdk = 36
        // CI passes its run number, so every build has a higher version and installs over the last one.
        val run = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 2
        versionCode = run
        versionName = "0.3.$run"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Tests on a phone or emulator check database migrations against the layouts kept in app/schemas.
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
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

    // The release-only "lint vital" pass took ~5.5 of the ~8.5 CI minutes (it re-scans every Compose
    // screen). Compile errors and the unit tests still fail the build.
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

// Room writes each database version's layout to app/schemas, kept in the repository, so a later version's migration
// is checked against the real earlier layout.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    // Tests that run on a phone or emulator (app/src/androidTest; .github/workflows/device-tests.yml).
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.01.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    // The activity-result code in MainActivity needs Fragment 1.3.0 or newer.
    implementation("androidx.fragment:fragment:1.8.5")
    implementation("androidx.core:core-ktx:1.15.0")
    // Payments are stored in an SQLite database (Db.kt).
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    // The app lock's fingerprint / face / PIN prompt.
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    // The screen's state holder (AppState) is a ViewModel.
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    // LocalLifecycleOwner moved here from Compose UI.
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    // The daily check's slow parts (weekly backup, recap, widget) run as background work.
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2025.01.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    // @Preview for the shared pieces (Previews.kt); the tooling that draws them is in debug builds only.
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
