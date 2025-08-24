plugins {
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    id("com.google.gms.google-services")
    id("kotlin-parcelize")
}

android {
    namespace = "com.samsung.health.mysteps"
    compileSdk = 36

    defaultConfig {
        minSdk = 30
        versionCode = 1

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    kotlin {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    implementation(libs.core.ktx)

    implementation(files("libs/samsung-health-data-api-1.0.0.aar"))

    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    //compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    debugImplementation(libs.ui.test.manifest)
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    debugImplementation(libs.ui.tooling)
    implementation(libs.material3)
    implementation(libs.activity.compose)
    //tests
    testImplementation(libs.mockk)
    testImplementation(libs.junit)
    testImplementation(libs.slf4j)
    implementation(libs.junit.ktx)

    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.viewmodel.compose)

    // WorkManager 라이브러리
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("androidx.compose.material:material-icons-extended:1.6.8")

    implementation(libs.gson)
    //hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // Firebase BoM (Bill of Materials) - 여러 Firebase 라이브러리 버전을 관리해줍니다.
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    // Firestore KTX 라이브러리 - 코틀린에서 Firestore를 더 쉽게 사용하게 해줍니다.
    implementation("com.google.firebase:firebase-firestore-ktx")
}
