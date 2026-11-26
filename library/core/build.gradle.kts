plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.sevtinge.hyperceiler.core"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = 35

        buildConfigField("String", "APP_MODULE_ID", "\"com.sevtinge.hyperceiler\"")
    }

    buildFeatures {
        buildConfig = true
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
    // api(projects.library.hook)
    api(projects.library.common)
    api(projects.library.provision)
    api(libs.appiconloader)
}
