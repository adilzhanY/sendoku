import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

/*
 * The upload key, read from outside the repository.
 *
 * Never from gradle.properties and never from an environment variable baked into a script:
 * both end up in a shell history or a build log sooner or later. The file lives in the user's
 * home with permissions 600, and its absence is not an error, it just leaves the release
 * unsigned so anybody can still clone this and build it.
 *
 * Point SENDOKU_KEYSTORE at a different properties file to sign with another key.
 */
val keystoreProperties: Properties? = loadKeystoreProperties()

fun loadKeystoreProperties(): Properties? {
    val path =
        providers.environmentVariable("SENDOKU_KEYSTORE").orNull
            ?: "${System.getProperty("user.home")}/.sendoku/keystore.properties"
    val file = File(path)
    if (!file.isFile) return null
    val properties = Properties()
    file.inputStream().use { properties.load(it) }
    return properties
}

android {
    namespace = "com.sendoku.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sendoku.app"
        minSdk = 26
        targetSdk = 37
        // Launch. The code is what Play orders updates by and can only ever go up; the name
        // is what a person reads. Bump the code on every upload, even a rejected one.
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // The store screenshot renders are not checks and take a while. tools/store-shots.sh
        // runs them on purpose; nothing else should have to wait for them.
        testInstrumentationRunnerArguments["notAnnotation"] = "com.sendoku.app.StoreShot"
        // en-XA doubles the length of every string and ar-XB mirrors the layout, both without
        // needing a translator. They are the cheapest way to find a layout that only breaks in
        // German or only breaks in Arabic.
        // Every language the app is written in, and nothing else. This list is what actually
        // ends up in the APK: a translation missing from here is stripped at package time,
        // and the app quietly falls back to English with no error anywhere.
        resourceConfigurations += listOf("en", "ru", "de", "tr", "es", "it", "ja", "fr", "en-rXA", "ar-rXB")
    }

    /*
     * Every language ships in every install.
     *
     * Play splits a bundle by language by default and downloads only the ones the phone is
     * set to. This app lets a player choose a language the phone is not set to, which is the
     * whole point of the picker, so a split install would offer seven languages and have the
     * strings for one. Turning the split off costs a couple of hundred kilobytes and is the
     * only setting under which the language picker is not a lie.
     */
    bundle {
        language {
            enableSplit = false
        }
    }

    signingConfigs {
        if (keystoreProperties != null) {
            create("upload") {
                storeFile = File(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                // v1 is the signature scheme from 2008 and minSdk here is 26, so the two
                // schemes that phones actually verify are the only two worth writing.
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        debug {
            // Generates en-XA, which doubles the length of every string, and ar-XB, which
            // mirrors the layout. Between them they find the layouts that only break in a
            // language nobody on the team reads.
            isPseudoLocalesEnabled = true
        }

        // The baseline profile plugin adds two more build types of its own, benchmarkRelease
        // and nonMinifiedRelease. Both are release with the debug signature and profiling
        // left on, which is what Macrobenchmark needs and what a hand written benchmark build
        // type would have duplicated.

        release {
            signingConfig = signingConfigs.findByName("upload")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        // The about screen shows the version, and reading it from here is the only way to
        // keep it in step with what was actually built.
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    /*
     * Lint, taken seriously.
     *
     * Hardcoded text and a missing translation both fail the build rather than warn. Both are
     * invisible until somebody switches language, and by then the app is shipped. The rest of
     * lint runs as warnings that also fail, so a warning cannot quietly become the norm.
     */
    lint {
        warningsAsErrors = true
        abortOnError = true
        checkDependencies = true
        error += listOf("HardcodedText", "MissingTranslation", "ExtraTranslation")
        // The pseudo locales are generated for testing and are deliberately not translated.
        disable += listOf("MissingQuantity")
        sarifReport = true
        textReport = true
    }

    // The exported schemas are also test assets, which is how MigrationTestHelper gets hold
    // of version 1 to upgrade from. Without this the migration test fails with a missing file
    // rather than a broken migration, which is a confusing way to learn nothing.
    sourceSets.getByName("androidTest") {
        assets.srcDirs(files("$projectDir/schemas"))
    }

    // Room writes the schema of every version here, and they are committed. Without the old
    // schema on disk there is nothing for a migration to migrate from.
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.generateKotlin", "true")
    }
}

/*
 * Compose compiler stability reports, on request:
 *
 *   ./gradlew :app:assembleRelease -PcomposeMetrics
 *
 * They answer the one question that matters for the board: does entering a digit in one cell
 * recompose that cell or all eighty one. A composable marked skippable with stable parameters
 * is the machine telling you it will not.
 */
if (project.hasProperty("composeMetrics")) {
    composeCompiler {
        metricsDestination = layout.buildDirectory.dir("compose-metrics")
        reportsDestination = layout.buildDirectory.dir("compose-reports")
    }
}

dependencies {
    implementation(project(":engine"))

    /*
     * Room's migration test helper reads the exported schema with kotlinx serialization, and
     * it needs a newer one than AndroidX lifecycle drags in. Because the test classpath is
     * resolved consistently with the app's, bumping it here is the only place that works: pin
     * it on the test side alone and the app still wins with 1.7.3, and the helper dies with an
     * AbstractMethodError on an interface that changed shape.
     */
    constraints {
        implementation(libs.kotlinx.serialization.core)
    }

    // Debug only. A sudoku app holds one board and one view model, so a leak here would have
    // to be something structural, and structural is exactly the kind that survives to release.
    debugImplementation(libs.leakcanary)
    baselineProfile(project(":benchmark"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.kotlinx.coroutines.test)
}

/*
 * Writes the list of everything the app is built out of, for the licences screen.
 *
 * Generated from the resolved dependency graph rather than typed by hand, so it cannot fall
 * behind what actually ships. Licence names come from the map below: reading them out of
 * every POM would be more machinery than four entries deserve, and everything here is either
 * Apache 2.0 or the font's own open licence.
 */
val licenceNames =
    mapOf(
        "androidx" to "Apache License 2.0",
        "com.google" to "Apache License 2.0",
        "org.jetbrains" to "Apache License 2.0",
        "com.squareup" to "Apache License 2.0",
        "org.jspecify" to "Apache License 2.0",
        "junit" to "Eclipse Public License 1.0",
    )

/*
 * The four bundled typefaces, one per theme.
 *
 * These are files in res/font rather than dependencies, so nothing resolves them for us and
 * they have to be named here. All four are under the SIL Open Font License, which asks for
 * the copyright notice to be carried with the software, and this screen is where it is
 * carried. Keep this in step with tools/subset-fonts.py.
 */
val fontLicences =
    listOf(
        "Inter, Copyright 2016 The Inter Project Authors|SIL Open Font License 1.1",
        "PT Serif, Copyright 2010 ParaType|SIL Open Font License 1.1",
        "Manrope, Copyright 2018 The Manrope Project Authors|SIL Open Font License 1.1",
        "JetBrains Mono, Copyright 2020 The JetBrains Mono Project Authors|SIL Open Font License 1.1",
    )

abstract class GenerateLicences : DefaultTask() {
    @get:Input
    abstract val artifacts: SetProperty<String>

    @get:Input
    abstract val names: MapProperty<String, String>

    @get:Input
    abstract val fonts: ListProperty<String>

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @TaskAction
    fun generate() {
        val lines =
            artifacts.get().sorted().map { artifact ->
                val licence =
                    names
                        .get()
                        .entries
                        .firstOrNull { artifact.startsWith(it.key) }
                        ?.value
                        ?: "see the project page"
                "$artifact|$licence"
            }
        val directory = output.get().asFile
        directory.mkdirs()
        val file = File(directory, "licences.txt")
        file.writeText(
            (fonts.get() + lines)
                .joinToString("\n", postfix = "\n"),
        )
    }
}

val generateLicences =
    tasks.register<GenerateLicences>("generateLicences") {
        group = "sendoku"
        description = "Writes the open source licence list the about screen reads."
        val resolved =
            configurations.named("releaseRuntimeClasspath").map { configuration ->
                configuration.incoming.resolutionResult.allComponents
                    .map { it.id.displayName }
                    .filter { it.contains(':') && !it.startsWith("project ") }
                    .map { it.substringBeforeLast(':') }
                    .toSet()
            }
        artifacts.set(resolved)
        names.set(licenceNames)
        fonts.set(fontLicences)
        output.set(layout.buildDirectory.dir("generated/licences"))
    }

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(generateLicences, GenerateLicences::output)
    }
}
