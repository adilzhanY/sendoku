package com.sendoku.engine.catalog

import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.Symmetry
import java.io.File

/**
 * Command line entry point for building a puzzle batch.
 *
 * Run it through the Gradle task rather than by hand:
 *
 * ```sh
 * ./gradlew :engine:generateCatalog --args="engine/src/main/resources/catalog/classic.sdkb 500"
 * ```
 *
 * With no arguments at all it rebuilds the shipped batch exactly: 500 of every grade,
 * rotational, seed 1.
 *
 * ```sh
 * ./gradlew :engine:generateCatalog
 * ```
 *
 * Arguments, all optional after the first: output file, puzzles per grade, the grades to
 * fill, the symmetry, and the seed. The seed is what makes a batch reproducible, so the
 * same command always writes the same file.
 */
public fun main(args: Array<String>) {
    val output = File(args.getOrElse(0) { "engine/src/main/resources/catalog/classic.sdkb" })
    val perGrade = args.getOrElse(1) { "500" }.toInt()
    val grades = args.getOrElse(2) { Grade.entries.joinToString(",") { it.name } }
        .split(",").map { Grade.valueOf(it.trim().uppercase()) }
    val symmetry = Symmetry.valueOf(args.getOrElse(3) { "ROTATIONAL" }.uppercase())
    val seed = args.getOrElse(4) { "1" }.toLong()

    val request = BatchRequest(
        dims = Dimensions.CLASSIC,
        targets = grades.associateWith { perGrade },
        symmetry = symmetry,
        seed = seed,
    )

    println("Generating $perGrade puzzles each of ${grades.joinToString { it.displayName }}")
    println("Symmetry $symmetry, seed $seed, ${request.workers} workers")

    val result = BatchRun.run(request)
    print(result.summary())

    if (!result.met(request)) {
        System.err.println("WARNING: the run gave up before filling every grade")
    }

    output.parentFile?.mkdirs()
    output.outputStream().use { PuzzleFormat.write(it, request.dims, result.puzzles) }
    println("wrote ${result.puzzles.size} puzzles to ${output.path}, ${output.length()} bytes")
}
