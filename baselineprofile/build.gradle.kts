plugins {
  id("com.android.test")
  alias(libs.plugins.androidx.baselineprofile)
}

android {
  namespace = "com.nexus.music.baselineprofile"
  compileSdk = 37

  defaultConfig {
    minSdk = 28
    targetSdk = 36
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  targetProjectPath = ":app"
}

baselineProfile {
  useConnectedDevices = false
}

dependencies {
  implementation("androidx.test.ext:junit:1.2.1")
  implementation("androidx.test:runner:1.6.2")
  implementation("androidx.test.uiautomator:uiautomator:2.3.0")
  implementation("androidx.benchmark:benchmark-macro-junit4:1.4.1")
}
