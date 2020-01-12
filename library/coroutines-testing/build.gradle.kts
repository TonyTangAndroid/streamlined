import io.github.reactivecircus.streamlined.build.libraries

plugins {
    `streamlined-plugin`
    id("kotlin")
}

dependencies {
    implementation(libraries.kotlinStdlib)
    implementation(libraries.junit)
    implementation(libraries.kotlinx.coroutines.core)
    implementation(libraries.kotlinx.coroutines.test)
}
