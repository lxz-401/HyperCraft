plugins {
    id("java-library")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.jvmToolchain.get().toInt())
    }
}

dependencies {
    implementation(libs.auto.service)
    annotationProcessor(libs.auto.service)
}
