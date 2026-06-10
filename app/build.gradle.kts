import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val gitCommitCount = providers.exec {
    commandLine("git", "rev-list", "--count", "HEAD")
}.standardOutput.asText.get().trim().toInt()

// -- Signing ----------------------------------------------------------------
// Credentials are read from local.properties (gitignored) so they never
// appear in source control. The release build will simply be unsigned if
// the file or any key is missing (safe for CI forks / open-source clones).
val localProps = Properties().also { props ->
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { props.load(it) }
}
val ksFile      = localProps.getProperty("KEYSTORE_FILE")
val ksPass      = localProps.getProperty("KEYSTORE_PASSWORD")
val ksAlias     = localProps.getProperty("KEY_ALIAS")
val ksKey       = localProps.getProperty("KEY_PASSWORD")
val versionNameStr = localProps.getProperty("VERSION_NAME") ?: "0.0.0"

android {
    namespace = "dev.heckr.kitsudo"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "dev.heckr.kitsudo"
        minSdk = 33
        targetSdk = 36
        versionCode = gitCommitCount
        versionName = versionNameStr

        testInstrumentationRunner = "com.google.dagger.hilt.android.testing.HiltTestRunner"
    }

    if (ksFile != null && ksPass != null && ksAlias != null && ksKey != null) {
        signingConfigs {
            create("release") {
                storeFile     = file(ksFile)
                storePassword = ksPass
                keyAlias      = ksAlias
                keyPassword   = ksKey
            }
        }
    }

    // Distribution channels. The two flavors differ only in their manifest:
    // `github` (sideloaded) declares the install/storage permissions the in-app
    // self-updater needs; `play` omits them because the Play Store handles
    // updates and Google Play restricts REQUEST_INSTALL_PACKAGES.
    flavorDimensions += "distribution"
    productFlavors {
        create("github") {
            dimension = "distribution"
            isDefault = true
        }
        create("play") {
            dimension = "distribution"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigningConfig = signingConfigs.findByName("release")
            if (releaseSigningConfig != null) {
                signingConfig = releaseSigningConfig
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            // These prebuilt AndroidX .so files cannot be stripped without the NDK.
            // They are already in their final form; keep them as-is to suppress the warning.
            keepDebugSymbols += setOf(
                "**/libandroidx.graphics.path.so",
                "**/libdatastore_shared_counter.so",
            )
        }
    }
}

androidComponents {
    onVariants { variant ->
        val versionName = android.defaultConfig.versionName ?: "0.0.0"
        val dashed = versionName.replace(Regex("-.*"), "").replace(".", "-")
        // `github` keeps the historical bare names; `play` is prefixed so the
        // two flavors' outputs never collide.
        val flavorPrefix = if (variant.flavorName == "play") "play-" else ""
        val apkFileName = when (variant.buildType) {
            "debug" -> "kitsudo-${flavorPrefix}dev-$dashed.apk"
            "release" -> "kitsudo-${flavorPrefix}release-$dashed.apk"
            else -> "ptdl-${variant.name}.apk"
        }
        variant.outputs.forEach { output ->
            if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
                output.outputFileName = apkFileName
            }
        }
    }
}

dependencies {
    // Shared domain + data layer
    implementation(project(":core"))
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose UI
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room runtime (needed by DatabaseModule which calls Room.databaseBuilder)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)

    // Wearable Data Layer (phone side)
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Drag-to-reorder lists
    implementation(libs.reorderable)

    // Debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Unit Tests
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented Tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.mockk.android)
    kspAndroidTest(libs.hilt.android.compiler)
}
