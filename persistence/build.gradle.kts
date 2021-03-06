import io.github.reactivecircus.streamlined.libraries

plugins {
    `streamlined-plugin`
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
    id("com.squareup.sqldelight")
}

sqldelight {
    database("StreamlinedDatabase") {
        schemaOutputDirectory = file("src/main/sqldelight/databases")
    }
}

dependencies {
    // Kotlin stdlib
    implementation(libraries.kotlinStdlib)

    // SQLDelight
    implementation(libraries.sqldelight.driver.android)
    implementation(libraries.sqldelight.coroutinesExtensions)

    // Coroutines
    implementation(libraries.kotlinx.coroutines.core)

    // Dagger
    implementation(libraries.dagger.runtime)
    kapt(libraries.dagger.compiler)

    // timber
    implementation(libraries.timber)

    // Unit tests
    testImplementation(libraries.junit)
    testImplementation(libraries.truth)
    testImplementation(libraries.sqldelight.driver.jvm)
    testImplementation(project(":coroutines-test-ext"))
}
