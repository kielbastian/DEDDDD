plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Pure Kotlin module — no Android, no framework dependencies.
dependencies {
    implementation(project(":core"))
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
