plugins {
    alias(libs.plugins.android.application)
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
    }

    buildTypes {
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

    // Room writes the schema of every version here, and they are committed. Without the old
    // schema on disk there is nothing for a migration to migrate from.
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.generateKotlin", "true")
    }
}

dependencies {
    implementation(project(":engine"))

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
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.kotlinx.coroutines.test)
}

/**
 * Writes the list of everything the app is built out of, for the licences screen.
 *
 * Generated from the resolved dependency graph rather than typed by hand, so it cannot fall
 * behind what actually ships. Licence names come from the map below: reading them out of
 * every POM would be more machinery than four entries deserve, and everything here is either
 * Apache 2.0 or the font's own open licence.
 */
val licenceNames = mapOf(
    "androidx" to "Apache License 2.0",
    "com.google" to "Apache License 2.0",
    "org.jetbrains" to "Apache License 2.0",
    "com.squareup" to "Apache License 2.0",
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
        val lines = artifacts.get().sorted().map { artifact ->
            val licence = names.get().entries
                .firstOrNull { artifact.startsWith(it.key) }
                ?.value
                ?: "see the project page"
            "$artifact|$licence"
        }
        val directory = output.get().asFile
        directory.mkdirs()
        val file = File(directory, "licences.txt")
        file.writeText(
            (listOf("Inter (digits only), by Rasmus Andersson|SIL Open Font License 1.1") + lines)
                .joinToString("\n", postfix = "\n"),
        )
    }
}

val generateLicences = tasks.register<GenerateLicences>("generateLicences") {
    group = "sendoku"
    description = "Writes the open source licence list the about screen reads."
    val resolved = configurations.named("releaseRuntimeClasspath").map { configuration ->
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
