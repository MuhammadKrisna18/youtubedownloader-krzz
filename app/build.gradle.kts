plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "com.personal.ytmusicdownloader"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.personal.ytmusicdownloader"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0.0"

    ndk {
      abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      signingConfig = signingConfigs.getByName("debug")
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  packaging {
    jniLibs {
      useLegacyPackaging = true
    }
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)

  implementation(libs.coil.compose)
  implementation(libs.okhttp)
  implementation(libs.kotlinx.coroutines.android)

  implementation(libs.youtubedl.library)
  implementation(libs.youtubedl.ffmpeg)

  debugImplementation(libs.androidx.compose.ui.tooling)
}
