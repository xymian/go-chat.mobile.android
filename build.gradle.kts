// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false

    id("com.google.devtools.ksp") version "2.2.0-2.0.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0"
}