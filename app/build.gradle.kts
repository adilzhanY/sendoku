plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.sendoku.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sendoku.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // The store screenshot renders are not checks and take a while. tools/store-shots.sh
        // runs them on purpose; nothing else should have to wait for them.
        testInstrumentationRunnerArguments["notAnnotation"] = "com.sendoku.app.StoreShot"
        // en-XA doubles the length of every string and ar-XB mirrors the layout, both without
        // needing a translator. They are the cheapest way to find a layout that only breaks in
        // German or only breaks in Arabic.
        resourceConfigurations += listOf("en", "ru", "en-rXA", "ar-rXB")
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
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

abstract class GenerateLicences : DefaultTask() {
    @get:Input
    abstract val artifacts: SetProperty<String>

    @get:Input
    abstract val names: MapProperty<String, String>

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
            (listOf("Inter (digits only), Copyright 2016 The Inter Project Authors|SIL Open Font License 1.1") + lines)
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
        output.set(layout.buildDirectory.dir("generated/licences"))
    }

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(generateLicences, GenerateLicences::output)
    }
}
