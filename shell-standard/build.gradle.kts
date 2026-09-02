// shell-standard — RTX-100 Standard Human Shell DFM (Phase 0.1 / Phase 1-2 toolset target)
//
// Dynamic Feature Module carrying the "Standard Human Shell" tier described
// in Hosted_Local_Shell.md v2 §Final 3-tier design: bash less nano tar gzip
// unzip curl ssh(openssh-client) ping grep sed awk find du df stat.
//
// This file only establishes the module and its Play Feature Delivery
// wiring (Phase 0.1). Binary sourcing/verification/manifest generation is
// Phase 1-2 scope and lands as separate commits once those binaries are
// pinned, licensed, and hashed.

plugins {
  alias(libs.plugins.android.dynamic.feature)
}

android {
  namespace = "com.inscopelabs.abx.binbox.shellstandard"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    minSdk = 24
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

dependencies {
  implementation(project(":app"))
}
