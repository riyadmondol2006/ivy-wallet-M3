plugins {
    id("com.android.library")
    id("kotlin-android")
    id("org.jetbrains.kotlin.android")
}

android {
    // Kotlin
    val javaVersion = catalog.version("jvm-target")
    compileOptions {
        sourceCompatibility = JavaVersion.valueOf("VERSION_$javaVersion")
        targetCompatibility = JavaVersion.valueOf("VERSION_$javaVersion")
    }

    kotlinOptions {
        jvmTarget = javaVersion
    }

    // Android
    compileSdk = catalog.version("compile-sdk").toInt()
    defaultConfig {
        minSdk = catalog.version("min-sdk").toInt()
    }

    lint {
        // The Slack compose-lint ParameterOrderDetector crashes (NPE in getText()) on some legacy
        // files, surfacing as fatal `LintError`. RestrictedApi fires ~2k false positives on Room
        // 2.7's generated DAO code (calls to @RestrictTo androidx.room.util.performSuspending).
        // Both are tooling issues with no source to fix — disable project-wide so every module's
        // lint report (and the app's checkDependencies aggregate) stays clean.
        disable += "ComposeParameterOrder"
        disable += "RestrictedApi"
    }
}

// Library verify*Resources links each module's resources in isolation, without the app theme, so
// it false-fails on app-provided theme attrs (e.g. ?attr/colorControlNormal used by shared vector
// drawables) and breaks `assembleRelease`. The app module's final resource link
// (processReleaseResources) still validates every resource in context, so this per-library check
// is redundant — disable it across all library/feature modules.
tasks.matching { it.name.startsWith("verify") && it.name.endsWith("Resources") }
    .configureEach { enabled = false }

gradle.projectsEvaluated {
    // Increase tests Heap Size because of Kotest property-based tests
    tasks.withType<Test> {
        maxHeapSize = "2048m"
    }
}

dependencies {
    implementation(libs.bundles.arrow)
    implementation(libs.bundles.kotlin)
    implementation(catalog.bundle("kotlin-android"))
    implementation(libs.timber)

    testImplementation(libs.bundles.testing)
}
