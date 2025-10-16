plugins {
    id("com.android.application") version "8.1.2" apply true
}

android {
    namespace = "com.connectsdk.sampler"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.connectsdk.sampler"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.txt"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://maven.aliyun.com/repository/public")
}

dependencies {
    implementation(project(":connectsdk"))
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
}