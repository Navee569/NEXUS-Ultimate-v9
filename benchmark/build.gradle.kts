plugins {
  id("com.android.test")
  id("org.jetbrains.kotlin.android")
}

android {
  namespace = "com.nexus.music.benchmark"
  compileSdk = 37

  defaultConfig {
    minSdk = 28
    targetSdk = 36
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  targetProjectPath = ":app"
  // Benchmark the optimized, non-debuggable FOSS app variant.
  targetVariant = "universalFossRelease"
}

dependencies {
  implementation("androidx.benchmark:benchmark-macro-junit4:1.4.1")
  implementation("androidx.test.ext:junit:1.2.1")
  implementation("androidx.test:runner:1.6.2")
  implementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
