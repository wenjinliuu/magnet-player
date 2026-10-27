import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose.compiler)
  alias(libs.plugins.kotlinx.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.room)
  alias(libs.plugins.aboutlibraries)
}

android {
  namespace = "com.wenjin.magnetplayer"
  compileSdk = 37

  defaultConfig {
    applicationId = "com.wenjin.magnetplayer"
    minSdk = 26
    targetSdk = 36
    // 版本号随提交数递增，保证新包总能覆盖安装
    versionCode = getCommitCount().toInt()
    versionName = "0.3.0"

    // 只给小米平板（arm64）用
    ndk {
      abiFilters += "arm64-v8a"
    }

    vectorDrawables {
      useSupportLibrary = true
    }

    buildConfigField("String", "GIT_SHA", "\"${getCommitSha()}\"")
    buildConfigField("int", "GIT_COUNT", getCommitCount())
    buildConfigField("boolean", "ENABLE_UPDATE_FEATURE", "false")
    buildConfigField("boolean", "SCOPED_STORAGE_ONLY", "false")
  }

  // 签名：CI 从仓库 Secrets 注入环境变量；没有配置时退回 debug 签名，保证始终能装
  val keystoreFile = System.getenv("KEYSTORE_FILE")?.let { file(it) }?.takeIf { it.exists() }
  signingConfigs {
    // 固定的 debug 证书（随仓库提交），保证没配 Secrets 时每次构建签名也一样、能覆盖安装
    named("debug") {
      storeFile = file("debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
    if (keystoreFile != null) {
      create("release") {
        storeFile = keystoreFile
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS")
        keyPassword = System.getenv("KEY_PASSWORD")
      }
    }
  }

  dependenciesInfo {
    includeInApk = false
    includeInBundle = false
  }

  buildTypes {
    named("release") {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
      ndk {
        debugSymbolLevel = "none"
      }
      signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
    }

    named("debug") {
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-${getCommitCount()}"
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
    viewBinding = true
    buildConfig = true
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      excludes += "META-INF/DEPENDENCIES"
      excludes += "META-INF/LICENSE*"
      excludes += "META-INF/NOTICE*"
      excludes += "META-INF/*.kotlin_module"
      excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
    }
    jniLibs {
      // 解压到 nativeLibraryDir，TorrServer 才能作为可执行文件运行
      useLegacyPackaging = true
      keepDebugSymbols += "**/libtorrserver.so"
    }
  }

  testOptions {
    unitTests.isIncludeAndroidResources = true
    unitTests.all {
      it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED", "--add-opens=java.base/java.io=ALL-UNNAMED")
    }
  }

  @Suppress("UnstableApiUsage")
  androidResources {
    generateLocaleConfig = true
  }
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll(
      "-Xcontext-parameters",
      "-Xannotation-default-target=param-property",
      "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
    )
    jvmTarget.set(JvmTarget.JVM_17)
  }
}

composeCompiler {
  includeSourceInformation = true
}

room {
  schemaDirectory("$projectDir/schemas")
}

dependencies {
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.material3.android)
  implementation("com.google.android.material:material:1.13.0")
  implementation(libs.androidx.compose.material)
  implementation(libs.androidx.ui.tooling.preview)
  debugImplementation(libs.androidx.ui.tooling)
  implementation(libs.androidx.appcompat)
  implementation("androidx.preference:preference-ktx:1.2.1")
  implementation(libs.androidx.material3.icons.extended)
  implementation(libs.androidx.compose.animation.graphics)
  implementation(libs.saveable)

  implementation(platform(libs.koin.bom))
  implementation(libs.bundles.koin)

  implementation(libs.aboutlibraries.compose.m3)


  implementation(libs.room.runtime)
  ksp(libs.room.compiler)
  implementation(libs.room.ktx)

  implementation(libs.kotlinx.immutable.collections)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.okhttp)

  implementation(libs.mpv.android)

  // 本地截图测试（Robolectric 在 JVM 上渲染界面，和设计稿对比），不参与 release 构建
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.robolectric:robolectric:4.17")
  testImplementation("androidx.compose.ui:ui-test-junit4")
  debugImplementation("androidx.compose.ui:ui-test-manifest")

}

// TorrServer 二进制由 scripts/fetch-torrserver.sh 下载，缺失时直接报错，避免打出不能播放磁力的 APK
val checkTorrServer by tasks.registering {
  val so = file("src/main/jniLibs/arm64-v8a/libtorrserver.so")
  doLast {
    if (!so.exists()) {
      throw GradleException("缺少 ${so.path}，请先运行 scripts/fetch-torrserver.sh")
    }
  }
}
tasks.named("preBuild") { dependsOn(checkTorrServer) }

/* ---------------- Git helpers ---------------- */

fun getCommitCount(): String =
  runCommand("git rev-list --count HEAD") ?: "0"

fun getCommitSha(): String =
  runCommand("git rev-parse --short HEAD") ?: "unknown"

fun runCommand(command: String): String? =
  try {
    val parts = command.split(' ')
    val process = ProcessBuilder(parts)
      .redirectErrorStream(true)
      .start()

    val output = process.inputStream
      .bufferedReader()
      .readText()
      .trim()

    process.waitFor()
    output.ifEmpty { null }
  } catch (e: Exception) {
    null
  }
