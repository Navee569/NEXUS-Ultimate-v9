plugins {
  id("com.android.library")
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "com.nexus.designsystem"
  compileSdk = 37

  defaultConfig { minSdk = 26 }

  buildFeatures { compose = true }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
}
dependencies {
  implementation(libs.compose.runtime)
  implementation(libs.compose.foundation)
  implementation(libs.compose.ui)
  implementation(libs.material3)
}
