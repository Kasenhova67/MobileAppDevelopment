// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version libs.versions.agp apply false
    kotlin("plugin.compose") version libs.versions.kotlin apply false
    id("com.google.gms.google-services") version "4.4.1" apply false
}