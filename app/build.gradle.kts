import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.replica.replicaisland"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.replica.replicaisland"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "1.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // First try environment variables (for CI)
            val envKeystoreFile = System.getenv("KEYSTORE_FILE")
            val envKeystorePassword = System.getenv("KEYSTORE_PASSWORD")
            val envKeyAlias = System.getenv("KEY_ALIAS")
            val envKeyPassword = System.getenv("KEY_PASSWORD")

            if (!envKeystoreFile.isNullOrEmpty() && !envKeystorePassword.isNullOrEmpty() &&
                !envKeyAlias.isNullOrEmpty() && !envKeyPassword.isNullOrEmpty()
            ) {
                storeFile = file(envKeystoreFile)
                storePassword = envKeystorePassword
                keyAlias = envKeyAlias
                keyPassword = envKeyPassword
            } else {
                // Fall back to signing.properties file (for local builds)
                val props = Properties()
                val propFile = file("../gradle/signing.properties")
                if (propFile.canRead()) {
                    props.load(FileInputStream(propFile))
                    if (props.containsKey("STORE_FILE") && props.containsKey("STORE_PASSWORD") &&
                        props.containsKey("KEY_ALIAS") && props.containsKey("KEY_PASSWORD")
                    ) {
                        storeFile = file(props["STORE_FILE"] as String)
                        storePassword = props["STORE_PASSWORD"] as String
                        keyAlias = props["KEY_ALIAS"] as String
                        keyPassword = props["KEY_PASSWORD"] as String
                    }
                }
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        resValues = true
    }
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.appcompat.resources)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.datastore.preferences)

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    testImplementation(libs.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    debugImplementation(libs.androidx.fragment.testing.manifest)
    androidTestImplementation(libs.androidx.fragment.testing)
}