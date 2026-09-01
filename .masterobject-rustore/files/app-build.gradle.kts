plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "ru.furniturecrm.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "ru.masterobject.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        val rustoreConsoleAppId = providers.gradleProperty("rustoreConsoleAppId").orElse("0").get()
        val rustoreProductId = providers.gradleProperty("rustoreProductId").orElse("master_object_full_199").get()
        buildConfigField("String", "RUSTORE_CONSOLE_APP_ID", "\"$rustoreConsoleAppId\"")
        buildConfigField("String", "RUSTORE_PRODUCT_ID", "\"$rustoreProductId\"")
        resValue("string", "CONSOLE_APPLICATION_ID", rustoreConsoleAppId)
        resValue("string", "rustore_pay_deeplink_scheme", "masterobjectpay")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "ENFORCE_PAYWALL", "false")
        }
        release {
            buildConfigField("boolean", "ENFORCE_PAYWALL", "true")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.06.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.7.0")

    implementation(platform("ru.rustore.sdk:bom:2025.11.01"))
    implementation("ru.rustore.sdk:pay")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
