plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":core"))
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

kotlin {
    jvmToolchain(17)
}
