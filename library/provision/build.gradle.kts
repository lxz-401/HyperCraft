plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.detekt)
}
android {

    namespace = "com.sevtinge.hyperceiler.provision"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = 35
    }

    buildFeatures {
        aidl = true
    }

    buildTypes {
        release {}
        create("beta") {}
        create("canary") {}
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
    api(projects.library.common)
    api(projects.library.libhook)
}
