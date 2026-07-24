plugins {
      alias(libs.plugins.android.application)
      alias(libs.plugins.kotlin.android)
      alias(libs.plugins.kotlin.serialization)
  }

  android {
      namespace = "com.vcam"
      compileSdk = 35

      defaultConfig {
          applicationId = "com.vcam"
          minSdk = 26
          targetSdk = 35
          versionCode = 1
          versionName = "1.0"

          testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

          ndk {
              abiFilters += listOf("armeabi-v7a", "arm64-v8a")
          }

          externalNativeBuild {
              cmake {
                  cppFlags += "-std=c++17"
                  arguments += "-DANDROID_STL=c++_shared"
              }
          }

          // Supabase config — set via GitHub Actions secrets or local.properties
          buildConfigField("String", "SUPABASE_URL", "\"${System.getenv("SUPABASE_URL") ?: project.findProperty("SUPABASE_URL") ?: ""}\"")
          buildConfigField("String", "SUPABASE_ANON_KEY", "\"${System.getenv("SUPABASE_ANON_KEY") ?: project.findProperty("SUPABASE_ANON_KEY") ?: ""}\"")
          // Google OAuth Web Client ID (from Google Cloud Console)
          buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${System.getenv("GOOGLE_WEB_CLIENT_ID") ?: project.findProperty("GOOGLE_WEB_CLIENT_ID") ?: ""}\"")
      }

      buildTypes {
          release {
              isMinifyEnabled = false
              proguardFiles(
                  getDefaultProguardFile("proguard-android-optimize.txt"),
                  "proguard-rules.pro"
              )
          }
          debug {
              isDebuggable = true
          }
      }

      compileOptions {
          sourceCompatibility = JavaVersion.VERSION_11
          targetCompatibility = JavaVersion.VERSION_11
      }

      kotlinOptions {
          jvmTarget = "11"
      }

      buildFeatures {
          viewBinding = true
          buildConfig = true
      }

      externalNativeBuild {
          cmake {
              path = file("src/main/jni/CMakeLists.txt")
              version = "3.22.1"
          }
      }

      packaging {
          jniLibs {
              keepDebugSymbols += setOf(
                  "**/vcplax.so",
                  "**/libvc.so",
                  "**/libshadowhook.so"
              )
          }
      }
  }

  dependencies {
      implementation(libs.androidx.core.ktx)
      implementation(libs.androidx.appcompat)
      implementation(libs.material)
      implementation(libs.androidx.constraintlayout)
      implementation(libs.androidx.gridlayout)
      implementation(libs.androidx.cardview)
      implementation(libs.androidx.lifecycle.viewmodel)
      implementation(libs.androidx.lifecycle.runtime)
      implementation(libs.androidx.activity.ktx)
      implementation(libs.libsu.core)
      implementation(libs.libsu.service)
      implementation(libs.glide)
      implementation(libs.kotlinx.coroutines.android)

      // Supabase
      implementation(libs.supabase.auth)
      implementation(libs.supabase.postgrest)
      implementation(libs.ktor.client.android)
      implementation(libs.ktor.client.core)
      implementation(libs.kotlinx.serialization.json)

      // Google Sign-In
      implementation(libs.google.play.services.auth)
      implementation(libs.androidx.credentials)
      implementation(libs.androidx.credentials.play)
      implementation(libs.googleid)
  }
