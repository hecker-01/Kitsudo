import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// versionCode tracks the commit count, like the phone app. The git reflog
// (appended on every commit, checkout, or reset) is wired in as a tracked input
// so the configuration cache re-evaluates the count whenever HEAD moves instead
// of reusing a stale value, which Play rejects as non-monotonic.
val gitReflog = providers.fileContents(
    rootProject.layout.projectDirectory.file(".git/logs/HEAD"),
).asText.orElse("")

val gitCommitCount = gitReflog.zip(
    providers.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
    }.standardOutput.asText,
) { _, count -> count.trim().toInt() }.get()

// Wear and phone ship as separate bundles under one Play listing, so their
// version codes must be unique. Keep Wear in a higher, non-overlapping range
// (offset) so Play prefers the Wear bundle on a watch when both could match, and
// so the two ranges never collide as the commit count grows.
val wearVersionCode = gitCommitCount + 1_000_000

// -- Signing ----------------------------------------------------------------
// Credentials are read from local.properties (gitignored) so they never
// appear in source control. The release build will simply be unsigned if
// the file or any key is missing (safe for CI forks / open-source clones).
val localProps = Properties().also { props ->
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { props.load(it) }
}
val ksFile  = localProps.getProperty("KEYSTORE_FILE")
val ksPass  = localProps.getProperty("KEYSTORE_PASSWORD")
val ksAlias = localProps.getProperty("KEY_ALIAS")
val ksKey   = localProps.getProperty("KEY_PASSWORD")
val versionNameStr = localProps.getProperty("VERSION_NAME") ?: "0.0.0"

android {
    namespace = "dev.heckr.kitsudo.wear"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "dev.heckr.kitsudo"
        minSdk = 33
        targetSdk = 36
        versionCode = wearVersionCode
        versionName = versionNameStr

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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
    }
}

androidComponents {
    onVariants { variant ->
        val versionName = android.defaultConfig.versionName ?: "0.0.0"
        val apkFileName = when (variant.buildType) {
            "debug" -> "kitsudo-wear-dev-${versionName.replace(Regex("-.*"), "").replace(".", "-")}.apk"
            "release" -> "kitsudo-wear-release-${versionName.replace(".", "-")}.apk"
            else -> "ptdl-${variant.name}.apk"
        }
        variant.outputs.forEach { output ->
            if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
                output.outputFileName = apkFileName
            }
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Shared domain + data layer
    implementation(project(":core"))

    // DataStore (needed directly for WearPreferencesModule Hilt binding)
    implementation(libs.androidx.datastore.preferences)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))

    // Wear Compose
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation)
    debugImplementation(libs.wear.tooling.preview)

    // Material icons (Check, etc.)
    implementation(libs.androidx.compose.material.icons.core)

    // Core Android
    implementation(libs.androidx.core.ktx)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room (watch-side local cache)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // DataStore (not needed on watch, but transitive via :core — no extra dep needed)

    // Wearable Data Layer
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)

    // Tiles + Complications (glanceable surfaces)
    implementation(libs.androidx.protolayout)
    implementation(libs.androidx.protolayout.material)
    implementation(libs.androidx.protolayout.expression)
    implementation(libs.androidx.tiles)
    implementation(libs.androidx.watchface.complications.data.source.ktx)
    // Bridges suspend functions to the ListenableFuture APIs the Tiles SDK expects.
    implementation(libs.kotlinx.coroutines.guava)
}
