import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform") version "2.4.10"
}

group = "dev.optosync.test"
version = "0.1.0"

kotlin {
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    js(IR) {
        browser {
            commonWebpackConfig {
                outputFileName = "opto-sync-kmp.js"
            }
        }
        binaries.executable()
    }

    linuxX64()
    macosX64()
    macosArm64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        val desktopMain by getting {
            kotlin.srcDir("vendor/opto-sync-clients/clients/kotlin/src/main/kotlin")
        }

        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
