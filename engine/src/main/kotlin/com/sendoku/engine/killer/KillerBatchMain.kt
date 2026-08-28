package com.sendoku.engine.killer

import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import java.io.File
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * Command line entry point for building a Killer batch.
 *
 * Run it through the Gradle task rather than by hand:
 *
 * ```sh
 * ./gradlew :engine:generateKillerCatalog
 * ```
 *
 * With no arguments it rebuilds the shipped Killer batch exactly: three hundred puzzles,
 * cages of two to four cells, seed one. The seed is what makes it reproducible, so the same
 * command always writes the same file.
 *
 * Unlike the classic batch this does not aim at a target count per grade. A Killer's
 * difficulty comes out of the cage layout rather than out of how many clues were removed, so
 * there is no dial to turn towards Hard: what there is is a size range, and what comes out is
 * whatever comes out. The job prints the spread it got so the number is measured rather than
 * hoped for.
 */
public fun main(args: Array<String>) {
    val output = File(args.getOrElse(0) { "engine/src/main/resources/catalog/killer.sdkk" })
    val count = args.getOrElse(1) { "300" }.toInt()
    val biggest = args.getOrElse(2) { "4" }.toInt()
    val seed = args.getOrElse(3) { "1" }.toLong()

    println("Generating $count Killer puzzles, cages of 2 to $biggest, seed $seed")

    val dims = Dimensions.CLASSIC
    val made = ArrayList<RatedKiller>(count)
    val skipped = HashMap<String, Int>()

    val millis = measureTimeMillis {
        val random = Random(seed)
        var attempts = 0
        while (made.size < count && attempts < count * ATTEMPT_HEADROOM) {
            attempts++
            val puzzle = KillerGenerator(dims, random, sizes = 2..biggest).next()
            if (puzzle == null) {
                skipped["no unique layout"] = (skipped["no unique layout"] ?: 0) + 1
                continue
            }
            val report = KillerRater(puzzle).solve()
            if (!report.isSolved) {
                // A Killer the ladder cannot finish is one the app cannot hint at, and
                // Sendoku does not ship a puzzle it cannot explain.
                skipped["beyond the ladder"] = (skipped["beyond the ladder"] ?: 0) + 1
                continue
            }
            made += RatedKiller(
                puzzle = puzzle,
                rating = report.rating,
                grade = report.grade,
                hardest = report.hardest,
                usage = report.usage,
            )
            if (made.size % PROGRESS_EVERY == 0) println("  ${made.size} of $count")
        }
    }

    output.parentFile?.mkdirs()
    output.outputStream().use { KillerFormat.write(it, dims, made) }

    val spread = Grade.entries.associateWith { grade -> made.count { it.grade == grade } }
        .filterValues { it > 0 }
        .entries.joinToString { "${it.key.displayName}=${it.value}" }
    println("Wrote ${made.size} puzzles to ${output.path}, ${output.length()} bytes, in ${millis}ms")
    println("Grades: $spread")
    if (skipped.isNotEmpty()) println("Skipped: ${skipped.entries.joinToString { "${it.key}=${it.value}" }}")
}

/** How many layouts to try per puzzle wanted before giving up on the whole batch. */
private const val ATTEMPT_HEADROOM = 4

private const val PROGRESS_EVERY = 50
