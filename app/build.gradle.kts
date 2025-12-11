plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

android {
    namespace = "com.ezeksapps.ezeksapp"
    compileSdk = 36
    ndkVersion = "29.0.13846066"

    defaultConfig {
        applicationId = "com.ezeksapps.ezeksapp"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // NDK Configuration
        externalNativeBuild {
            cmake {
                cppFlags.add("-std=c++17")
                cppFlags.add("-frtti")
                cppFlags.add("-fexceptions")
                arguments.add("-DANDROID_STL=c++_shared")
            }
        }

        ndk {
            // Filter ABIs
            abiFilters.addAll(listOf("arm64-v8a"))
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ADD SIGNING CONFIGS
    signingConfigs {
        getByName("debug") {
            // Debug signing is usually fine
        }
        create("release") {
            // Use debug key for testing release builds
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            // Reduce debug overhead for testing
            enableAndroidTestCoverage = false
            isPseudoLocalesEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug") // Use debug signing
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "4.0.3" // Must match installed version
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
            freeCompilerArgs.addAll(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" +
                        project.layout.buildDirectory.dir("compose_metrics").get().asFile.absolutePath
            )
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // navigation
    implementation(libs.androidx.navigation.compose)

    // dataStore
    implementation(libs.androidx.datastore.preferences)

    //hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.gson)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // JSON serialisation
    implementation(libs.kotlinx.serialization.json)

    // .md rendering
    implementation(libs.compose.markdown)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}