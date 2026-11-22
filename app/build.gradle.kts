plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "app.shabbosends"
    compileSdk = 36

    defaultConfig {
        // Kept from when the app was called Shabbos Ends, so updates install over
        // the old app and keep its settings, permissions and Samsung routines.
        applicationId = "app.shabbosends"
        minSdk = 36          // Live Updates need Android 16
        targetSdk = 36
        versionCode = 12
        versionName = "2.1"
    }

    // A fixed key committed to the repo, so every build from GitHub Actions
    // installs over the previous one. Keep the repo private.
    signingConfigs {
        create("sideload") {
            storeFile = file("sideload.jks")
            storePassword = "shabbosends"
            keyAlias = "shabbos"
            keyPassword = "shabbosends"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("sideload")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("com.kosherjava:zmanim:2.5.0")
}
