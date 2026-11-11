plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "br.com.devstudioia"
 compileSdk = 35
 defaultConfig {
  applicationId = "br.com.devstudioia"
  minSdk = 26
  targetSdk = 35
  versionCode = 1
  versionName = "1.0-alpha"
  manifestPlaceholders["appAuthRedirectScheme"] = "br.com.devstudioia"
 }
 buildTypes { release { isMinifyEnabled = false } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies {
 implementation("androidx.activity:activity-ktx:1.9.3")
 implementation("androidx.browser:browser:1.8.0")
 implementation("net.openid:appauth:0.11.1")
 implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
