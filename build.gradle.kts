plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
}

/*
 * Formatting, settled once so it never has to be discussed again.
 *
 * ktlint with the official Kotlin style, applied to every module from the root. The point is
 * not that this particular style is best. It is that a diff should show what changed rather
 * than where somebody's editor put the braces.
 */
spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(
            mapOf(
                // Trailing commas make a one line diff out of adding an argument.
                "ktlint_standard_trailing-comma-on-call-site" to "enabled",
                "ktlint_standard_trailing-comma-on-declaration-site" to "enabled",
                // Composables are named like types, which is the Compose convention and not
                // something ktlint knows about.
                "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                "ktlint_standard_function-naming" to "disabled",
                // Test names read as sentences in backticks, which is the whole point of them.
                "ktlint_standard_backing-property-naming" to "disabled",
                "max_line_length" to "120",
            ),
        )
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
    }
}

/*
 * Detekt, for the things formatting cannot see.
 *
 * Configured from `config/detekt.yml` so the rules are visible and arguable rather than
 * whatever the defaults happen to be this release.
 */
detekt {
    parallel = true
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt.yml"))
    source.setFrom(files("engine/src", "app/src"))
}

/*
 * A warning nobody fixes is a warning nobody reads.
 *
 * Kotlin warnings fail the build in both modules, the same way lint warnings already do.
 * The point is not that every warning matters. It is that once one is allowed to sit there,
 * the next hundred are invisible.
 */
subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            allWarningsAsErrors.set(true)
        }
    }
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "21"
    reports {
        html.required.set(true)
        sarif.required.set(true)
        md.required.set(false)
        txt.required.set(false)
    }
}
