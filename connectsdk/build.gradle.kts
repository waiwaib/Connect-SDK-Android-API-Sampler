plugins {
    id("com.android.library")
}

android {
    namespace = "com.connectsdk"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        targetSdk = 35
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://maven.aliyun.com/repository/public")
}

dependencies {
    implementation(fileTree("src/main/libs") { include("*.jar") })
    // AndroidX base
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("androidx.mediarouter:mediarouter:1.7.0")

    // Google Cast
    implementation("com.google.android.gms:play-services-cast:22.1.0")

    // Misc dependencies
    implementation("com.googlecode.plist:dd-plist:1.23")
    implementation("net.i2p.crypto:eddsa:0.3.0")
}