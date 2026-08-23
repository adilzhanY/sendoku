plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
    explicitApi()
}

dependencies {
    testImplementation(libs.kotlin.test)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
    }
}

/**
 * The engine is pure Kotlin on the JVM and has to stay that way.
 *
 * That rule is load bearing rather than stylistic. It is what keeps the whole test suite
 * running in a couple of seconds instead of waiting on an emulator, and it is what will
 * let the same solver run in a batch job that generates puzzles offline. One stray
 * androidx import would quietly cost both, so the build refuses it.
 */
abstract class CheckNoAndroidImports : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val importOfAndroid = Regex("""^\s*import\s+androidx?\.""")
        val offenders = sources.files
            .filter { it.isFile }
            .flatMap { file ->
                file.readLines().withIndex()
                    .filter { (_, line) -> importOfAndroid.containsMatchIn(line) }
                    .map { (number, line) -> "${file.path}:${number + 1}  ${line.trim()}" }
            }
        if (offenders.isNotEmpty()) {
            throw GradleException(
                "The engine module must not depend on Android:\n" + offenders.joinToString("\n") { "  $it" },
            )
        }
        report.get().asFile.writeText("no android imports in ${sources.files.size} files\n")
    }
}

val checkNoAndroidImports = tasks.register<CheckNoAndroidImports>("checkNoAndroidImports") {
    group = "verification"
    description = "Fails the build if the engine imports anything from Android."
    sources.from(fileTree("src") { include("**/*.kt") })
    report.set(layout.buildDirectory.file("reports/no-android-imports.txt"))
}

tasks.named("check") { dependsOn(checkNoAndroidImports) }
