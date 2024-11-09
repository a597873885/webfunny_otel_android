// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // keep this version in sync with /buildSrc/build.gradle.kts
        classpath("com.android.tools.build:gradle:8.3.2")
        classpath("com.github.dcendents:android-maven-gradle-plugin:2.1")
    }
}

plugins {
    id("webfunny.spotless-conventions")
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
        }
        maven {
            url = uri("https://jitpack.io")
        }
    }
    if (findProperty("release") != "true") {
        version = "$version-SNAPSHOT"
    }
}

subprojects {
    apply(plugin = "webfunny.spotless-conventions")
}
