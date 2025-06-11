import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.example.umbrellaalert"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.umbrellaalert"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // API Keys from local.properties
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }

        // OpenWeather API Key (날씨 정보)
        val weatherApiKey = localProperties.getProperty("weather.api.service.key") ?: "bef3d511dc00345ed56204adcf073d16"
        buildConfigField("String", "WEATHER_API_SERVICE_KEY", "\"$weatherApiKey\"")
        buildConfigField("String", "OPENWEATHER_API_KEY", "\"$weatherApiKey\"")

        // Bus API Service Key (버스 정보)
        val busApiKey = localProperties.getProperty("bus.api.service.key") ?: "VJ9IZb8N%2BRRUt%2Bl%2FtdMwuR2gO2W%2FyER8etH1%2FlCcR3q0c4AvOiXSItNi9hcNAfyrQOMTVvOkE0wJwTxnXZ0PDA%3D%3D"
        buildConfigField("String", "BUS_API_SERVICE_KEY", "\"$busApiKey\"")

        // Naver Map API Key (지도 정보)
        val naverMapClientId = localProperties.getProperty("naver.map.client.id") ?: "okua9z6cuf"
        val naverMapClientSecret = localProperties.getProperty("naver.map.client.secret") ?: ""
        buildConfigField("String", "NAVER_MAP_CLIENT_ID", "\"$naverMapClientId\"")
        buildConfigField("String", "NAVER_MAP_CLIENT_SECRET", "\"$naverMapClientSecret\"")

        // AndroidManifest.xml에 네이버 지도 클라이언트 ID 추가
        manifestPlaceholders["NAVER_MAP_CLIENT_ID"] = naverMapClientId
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // CardView
    implementation("androidx.cardview:cardview:1.0.0")

    // Material Design
    implementation("com.google.android.material:material:1.11.0")

    // ConstraintLayout
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.7.0")

    // Fragment
    implementation("androidx.fragment:fragment:1.6.2")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment:2.7.6")
    implementation("androidx.navigation:navigation-ui:2.7.6")

    // Hilt for Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48")
    annotationProcessor("com.google.dagger:hilt-compiler:2.48")

    // Gson for JSON parsing (KmaApiClient에서 사용)
    implementation("com.google.code.gson:gson:2.10.1")

    // 네이버클라우드 플랫폼 지도 SDK
    implementation("com.naver.maps:map-sdk:3.21.0")

    // Google Play Services Location (위치 서비스)
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // HTTP 클라이언트 (네이버 Directions API용)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}