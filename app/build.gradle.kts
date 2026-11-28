plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.kanso.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kanso.app"
        minSdk = 26
        targetSdk = 35
        versionCode = System.getenv("KANSO_VERSION_CODE")?.toIntOrNull() ?: 35
        versionName = System.getenv("KANSO_VERSION_NAME")?.takeIf { it.isNotBlank() } ?: "1.16.3"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
                ?: (findProperty("KANSO_KEYSTORE_PATH") as String?)
            val keystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                ?: (findProperty("KANSO_KEYSTORE_PASSWORD") as String?)
            val keyAliasProp = System.getenv("ANDROID_KEY_ALIAS")
                ?: (findProperty("KANSO_KEY_ALIAS") as String?)
            val keyPasswordProp = System.getenv("ANDROID_KEY_PASSWORD")
                ?: (findProperty("KANSO_KEY_PASSWORD") as String?)

            if (!keystorePath.isNullOrBlank() &&
                !keystorePassword.isNullOrBlank() &&
                !keyAliasProp.isNullOrBlank() &&
                !keyPasswordProp.isNullOrBlank()
            ) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                keyAlias = keyAliasProp
                keyPassword = keyPasswordProp
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:security"))
    implementation(project(":core:backup"))
    implementation(project(":core:ui"))
    implementation(project(":feature:today"))
    implementation(project(":feature:money"))
    implementation(project(":feature:brain"))
    implementation(project(":feature:habits"))
    implementation(project(":feature:lifeadmin"))
    implementation(project(":feature:utilities"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.play.services.mlkit.text.recognition)
    implementation(libs.play.services.base)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    testImplementation(libs.junit)
}
