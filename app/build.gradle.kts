// file:noinspection DependencyNotationArgument
import com.android.build.api.dsl.ApplicationBuildType
import com.android.build.gradle.tasks.PackageAndroidArtifact
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

val apkId = "HyperCeiler"
val gitHash: String by lazy { runGitCommand("rev-parse", "--short", "HEAD") ?: "unknown" }
val gitHashLong: String by lazy { runGitCommand("rev-parse", "HEAD") ?: "unknown" }
val gitCommitCount: Int by lazy { runGitCommand("rev-list", "--count", "HEAD")?.toIntOrNull() ?: 0 }
val gitBranch: String by lazy {
    val url = runGitCommand("remote", "get-url", "origin") ?: "unknown"
    val branch = runGitCommand("branch", "--show-current") ?: "unknown"
    """github\.com[:/](.+?)(\.git)?$""".toRegex().find(url)?.groupValues?.get(1).orEmpty() + "/" + branch
}
val gitVersionCode: Int by lazy { 5 + gitCommitCount }

fun runGitCommand(vararg args: String): String? = runCatching {
    ProcessBuilder(listOf("git") + args)
        .redirectErrorStream(true)
        .start()
        .let { process ->
            val output = process.inputStream.bufferedReader().readText().trim()
            if (process.waitFor() == 0 && output.isNotBlank()) output else null
        }
}.getOrNull()

fun loadPropertiesFromFile(fileName: String): Properties? =
    rootProject.file(fileName).takeIf { it.isFile }?.let { file ->
        Properties().apply { file.inputStream().use { load(it) } }
    }

android {
    namespace = "com.sevtinge.hyperceiler"
    compileSdk = libs.versions.compileSdk.get().toInt()
    compileSdkMinor = libs.versions.compileSdkMinor.get().toInt()
    buildToolsVersion = libs.versions.buildToolsVersion.get()

    defaultConfig {
        applicationId = namespace
        minSdk = 35
        targetSdk = 37
        versionCode = gitVersionCode
        versionName = "2.11.170"

        // Reproducible build: only include deterministic build info
        // Build time, OS, user, Java version are intentionally omitted
        val buildConfigData = mapOf(
            "GIT_HASH" to gitHash,
            "GIT_BRANCH" to gitBranch,
            "GIT_VERSION_CODE" to gitVersionCode.toString()
        )

        for ((key, value) in buildConfigData) {
            buildConfigField("String", key, "\"$value\"")
        }

        ndk {
            // noinspection ChromeOsAbiSupport
            abiFilters += "arm64-v8a"
        }

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++20", "-fvisibility=hidden")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    buildFeatures {
        aidl = true
        buildConfig = true
    }

    androidResources {
        additionalParameters += listOf("--allow-reserved-package-id", "--package-id", "0x36")
    }

    packaging {
        resources {
            merges += listOf("META-INF/xposed/*")
            excludes += listOf("**")
        }
        jniLibs {
            useLegacyPackaging = false
        }
        dex {
            useLegacyPackaging = true
        }
    }

    val signingProperties = loadPropertiesFromFile("signing.properties")
    fun signingValue(propertyName: String, environmentName: String): String? =
        signingProperties?.getProperty(propertyName)?.takeIf { it.isNotBlank() }
            ?: providers.environmentVariable(environmentName).orNull?.takeIf { it.isNotBlank() }

    val signingStorePath = signingValue("storeFile", "STORE_FILE")
    val signingStorePassword = signingValue("storePassword", "STORE_PASSWORD")
    val signingKeyAlias = signingValue("keyAlias", "KEY_ALIAS")
    val signingKeyPassword = signingValue("keyPassword", "KEY_PASSWORD")
    val customSigningConfig = if (signingStorePath != null && signingStorePassword != null &&
        signingKeyAlias != null && signingKeyPassword != null
    ) {
        signingConfigs.create("hasProperties") {
            storeFile = file(signingStorePath)
            storePassword = signingStorePassword
            keyAlias = signingKeyAlias
            keyPassword = signingKeyPassword
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    } else {
        null
    }

    buildTypes {
        val configSigning: ApplicationBuildType.() -> Unit = {
            signingConfig = customSigningConfig ?: signingConfigs.getByName("debug")
        }

        val applyBase: ApplicationBuildType.() -> Unit = {
            optimization.enable = true
            buildConfigField("String", "GIT_CODE", "\"$gitVersionCode\"")
        }

        // Reproducible: use git hash instead of timestamps
        release {
            applyBase()
            configSigning()
            buildConfigField("String", "GIT_HASH", "\"$gitHash\"")
            versionNameSuffix = "-r${gitVersionCode}"
        }

        create("beta") {
            applyBase()
            configSigning()
            buildConfigField("String", "GIT_HASH", "\"$gitHashLong\"")
            versionNameSuffix = "-beta-r${gitVersionCode}"
        }

        create("canary") {
            applyBase()
            configSigning()
            buildConfigField("String", "GIT_HASH", "\"$gitHashLong\"")
            versionNameSuffix = "-canary-${gitHash}-r${gitVersionCode}"
        }

        debug {
            configSigning()
            isMinifyEnabled = false
            buildConfigField("String", "GIT_HASH", "\"$gitHashLong\"")
            buildConfigField("String", "GIT_CODE", "\"$gitVersionCode\"")
            versionNameSuffix = "-debug-${gitHash}-r${gitVersionCode}"
        }
    }

    lint {
        toolchain {
            languageVersion = JavaLanguageVersion.of(libs.versions.jvmToolchain.get().toInt())
        }
    }
}

afterEvaluate {
    base {
        val buildTypeName = gradle.startParameter.taskNames
            .firstOrNull { it.contains("assemble", ignoreCase = true) }
            ?.substringAfterLast(":")
            ?.replace("assemble", "", ignoreCase = true)
            ?.lowercase() ?: "debug"
        val suffix = android.buildTypes.findByName(buildTypeName)?.versionNameSuffix ?: ""
        archivesName.set("$apkId-${android.defaultConfig.versionName}$suffix")
    }
}

// https://stackoverflow.com/a/77745844
tasks.withType<PackageAndroidArtifact> {
    doFirst { appMetadata.asFile.orNull?.writeText("") }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.jvmToolchain.get().toInt())
    }
}

kotlin.jvmToolchain(libs.versions.jvmToolchain.get().toInt())

dependencies {
    implementation(libs.viewpager2)
    implementation(projects.library.core)
    implementation(projects.library.common)

    api (libs.room.runtime)
    // FTS 支持
    api (libs.room.ktx)
    annotationProcessor (libs.room.compiler)
}

/**
 * Host-side native tests for the NativeHookRuntime (app/src/main/cpp/nativehook/)
 * and the desktop Dart hook's host-testable layers.
 *
 * These run on the build machine, not on Android: they need no device and no
 * Android SDK. On Linux they also build the ARM64 assembly harness test; on
 * macOS that single test is skipped by the script (eventfd / GNU as).
 *
 * Usage: ./gradlew :app:hostNativeTests
 * Optional: ./gradlew :app:hostNativeTests -PlibappSo=/path/to/libapp.so
 */
tasks.register<Exec>("hostNativeTests") {
    group = "verification"
    description = "Compiles and runs the host native tests (no device required)"
    workingDir = rootProject.projectDir
    val script = rootProject.file("tests/run_host_native_tests.sh")
    commandLine(buildList {
        add("bash")
        add(script.absolutePath)
        (project.findProperty("libappSo") as String?)?.let { add(it) }
    })
}

// detekt configuration
detekt {
    config.setFrom(rootProject.file("detekt.yml"))
    buildUponDefaultConfig = true
    ignoreFailures = false
}

// ktlint configuration
ktlint {
    android = true
    filter {
        excludes(
            "**/build/**",
            "**/kotlin/**",
            "**/gradle/**",
        )
    }
}
