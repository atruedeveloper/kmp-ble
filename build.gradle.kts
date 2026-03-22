plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.vanniktech.publish)
}

group = "com.atruedev"
version = providers.environmentVariable("VERSION").getOrElse("0.0.0-local")

kotlin {
    explicitApi()

    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-opt-in=kotlinx.cinterop.BetaInteropApi")
                    freeCompilerArgs.add("-opt-in=kotlinx.cinterop.ExperimentalForeignApi")
                }
            }
        }
    }

    android {
        namespace = "com.atruedev.kmpble"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()

        withHostTestBuilder {}.configure {}
    }

    jvm()

    val xcf = XCFramework("KmpBle")
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "KmpBle"
            isStatic = true
            xcf.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.core)
            implementation(libs.androidx.startup)
            runtimeOnly(project(":kmp-ble-quirks"))
        }
    }
}

// KMP 2.1+ new Android DSL absorbs the AGP extension, so consumerProguardFiles
// isn't directly configurable. Inject the rules into the AAR at bundle time.
tasks.withType<Zip>().matching { it.name == "bundleAndroidMainAar" }.configureEach {
    from("src/androidMain/consumer-rules.pro") {
        rename { "proguard.txt" }
    }
}

// Alias for CI: `assembleXCFramework` → Kotlin's built-in task
tasks.register("assembleXCFramework") {
    dependsOn("assembleKmpBleReleaseXCFramework")
    group = "build"
    description = "Assembles KmpBle.xcframework from iOS release frameworks"
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates("com.atruedev", "kmp-ble", version.toString())

    pom {
        name.set("kmp-ble")
        description.set("Kotlin Multiplatform BLE library for Android, iOS, and JVM")
        url.set("https://github.com/atruedeveloper/kmp-ble")
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }
        developers {
            developer {
                id.set("atruedeveloper")
                name.set("Gary Quinn")
                email.set("gary@atruedev.com")
            }
        }
        scm {
            url.set("https://github.com/atruedeveloper/kmp-ble")
            connection.set("scm:git:git://github.com/atruedeveloper/kmp-ble.git")
            developerConnection.set("scm:git:ssh://github.com/atruedeveloper/kmp-ble.git")
        }
    }
}
