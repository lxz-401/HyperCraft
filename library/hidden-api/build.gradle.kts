plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.android.internal"
    compileSdk = libs.versions.compileSdk.get().toInt()

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        create("beta") {
            isMinifyEnabled = false
        }
        create("canary") {
            isMinifyEnabled = false
        }
        debug {
            isMinifyEnabled = false
        }
    }
    lint {
        toolchain {
            languageVersion = JavaLanguageVersion.of(libs.versions.jvmToolchain.get().toInt())
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.jvmToolchain.get().toInt())
    }
}

dependencies {
    implementation(libs.annotation)
}
