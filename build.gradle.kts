plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}

// Builds every App Bundle a Play Store release needs: the phone `play` flavor
// plus the Wear OS app. Play has no single combined bundle, it distributes the
// two as separate AABs under one listing (they share the applicationId) and
// serves each to the matching device form factor. Run `./gradlew playReleaseBundles`.
tasks.register("playReleaseBundles") {
    group = "release"
    description = "Builds the phone (play flavor) and Wear OS App Bundles for a Play Store release."
    dependsOn(":app:bundlePlayRelease", ":wear:bundleRelease")
}
