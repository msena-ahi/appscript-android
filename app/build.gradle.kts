plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "br.com.devstudioia"
    compileSdk = 35
    defaultConfig {
        applicationId = "br.com.devstudioia"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.2-alpha"
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.isIncludeAndroidResources = true }
}
dependencies {
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("com.google.android.gms:play-services-auth:22.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.robolectric:robolectric:4.14.1")
}
