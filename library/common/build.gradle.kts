plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.detekt)
}

val statusBarFollowBuildAvailable = providers.gradleProperty("statusBarFollow").map(String::toBoolean).getOrElse(false)
val statusBarFollowServerBuildAvailable = providers.gradleProperty("statusBarFollowServer").map(String::toBoolean).getOrElse(false)

android {
    namespace = "com.sevtinge.hyperceiler.common"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = 35

        buildConfigField("String", "APP_MODULE_ID", "\"com.sevtinge.hyperceiler\"")
        buildConfigField("boolean", "STATUS_BAR_FOLLOW_AVAILABLE", "true")
        buildConfigField("boolean", "STATUS_BAR_FOLLOW_SERVER_AVAILABLE", "true")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "STATUS_BAR_FOLLOW_AVAILABLE", "true")
            buildConfigField("boolean", "STATUS_BAR_FOLLOW_SERVER_AVAILABLE", statusBarFollowServerBuildAvailable.toString())
        }
        release {
            buildConfigField("boolean", "STATUS_BAR_FOLLOW_AVAILABLE", statusBarFollowBuildAvailable.toString())
            buildConfigField("boolean", "STATUS_BAR_FOLLOW_SERVER_AVAILABLE", statusBarFollowServerBuildAvailable.toString())
        }
        create("beta") {
            buildConfigField("boolean", "STATUS_BAR_FOLLOW_AVAILABLE", statusBarFollowBuildAvailable.toString())
            buildConfigField("boolean", "STATUS_BAR_FOLLOW_SERVER_AVAILABLE", statusBarFollowServerBuildAvailable.toString())
        }
        create("canary") {
            buildConfigField("boolean", "STATUS_BAR_FOLLOW_AVAILABLE", statusBarFollowBuildAvailable.toString())
            buildConfigField("boolean", "STATUS_BAR_FOLLOW_SERVER_AVAILABLE", statusBarFollowServerBuildAvailable.toString())
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

kotlin.jvmToolchain(libs.versions.jvmToolchain.get().toInt())

dependencies {
    api(libs.bundles.miuix)
    // libxposed API 102
    compileOnlyApi(libs.libxposed.api)

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("org.robolectric:robolectric:4.13")
}
