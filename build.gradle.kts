plugins {
    id("com.android.application") version "8.9.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    // Generates the database code for Room; its version follows Kotlin's (2.0.21-...).
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}
