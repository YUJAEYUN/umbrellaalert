plugins {
    alias(libs.plugins.android.application)
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
    // ViewBinding
    buildFeatures {
        viewBinding = true
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
    implementation("androidx.recyclerview:recyclerview:1.2.1")

// CardView
    implementation("androidx.cardview:cardview:1.0.0")

// Material Design
    implementation("com.google.android.material:material:1.4.0")

// ConstraintLayout
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")

}
