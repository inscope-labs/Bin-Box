plugins {
  alias(libs.plugins.android.dynamic.feature)
}

android {
  namespace = "com.inscopelabs.abx.binbox.shellextended"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    minSdk = 24
  }
}

dependencies {
  implementation(project(":app"))
}
