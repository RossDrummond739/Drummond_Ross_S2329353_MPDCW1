plugins {
    alias(libs.plugins.android.application)
    // Reads API keys from local.properties (not committed) - see protectingYourKeys guide
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "org.me.gcu.drummond_ross_s2329353"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.me.gcu.drummond_ross_s2329353"
        minSdk = 26          // Android 8.0 (Oreo)
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Signed with the debug key so the release APK installs directly for marking.
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = false
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

secrets {
    // Real keys go in local.properties (git-ignored). This committed file only holds
    // empty placeholders so the project still builds for anyone who clones it.
    propertiesFileName = "local.properties"
    defaultPropertiesFileName = "local.defaults.properties"
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.fragment)
    implementation(libs.swiperefreshlayout)
    implementation(libs.play.services.maps)

    testImplementation(libs.junit)
    // kXML is the same XmlPullParser implementation Android uses; lets the parser run in JVM unit tests
    testImplementation(libs.kxml2)
    // Robolectric runs the real UI on the JVM for the navigation / rotation tests
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
}
