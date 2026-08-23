package com.sendoku.engine.catalog

import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.Symmetry
import com.sendoku.engine.technique.TechniqueSolver
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/** What a batch run is trying to produce. */
public data class BatchRequest(
    val dims: Dimensions = Dimensions.CLASSIC,
    val targets: Map<Grade, Int>,
    val symmetry: Symmetry = Symmetry.ROTATIONAL,
    val seed: Long = 1L,
    val workers: Int = Runtime.getRuntime().availableProcessors(),
    /** Gives up rather than spinning forever if a grade turns out to be unreachable. */
    val maxAttempts: Long = 2_000_000L,
) {
    init {
        require(targets.isNotEmpty()) { "a batch with no targets would do nothing" }
        require(targets.values.all { it > 0 }) { "every target must be positive" }
        require(workers >= 1) { "need at least one worker" }
    }
}

/** What a batch run produced, and what it cost. */
public data class BatchResult(
    val puzzles: List<RatedPuzzle>,
    val attempts: Long,
    val unrateable: Long,
    val elapsedMillis: Long,
) {
    val counts: Map<Grade, Int> get() = puzzles.groupingBy { it.grade }.eachCount()

    public fun met(request: BatchRequest): Boolean =
        request.targets.all { (grade, wanted) -> (counts[grade] ?: 0) >= wanted }

    /** A one line summary per grade, for the console. */
    public fun summary(): String = buildString {
        appendLine("generated ${puzzles.size} puzzles from $attempts attempts in ${elapsedMillis}ms")
        appendLine("$unrateable were beyond the ladder and were dropped")
        for (grade in Grade.entries) {
            val list = puzzles.filter { it.grade == grade }
            if (list.isEmpty()) continue
            val clues = list.map { it.clueCount }
            appendLine(
                "  ${grade.displayName.padEnd(11)} ${list.size.toString().padStart(5)}" +
                    "  clues ${clues.min()}..${clues.max()}" +
                    "  rating ${"%.2f".format(list.minOf { it.rating })}" +
                    "..${"%.2f".format(list.maxOf { it.rating })}",
            )
        }
    }
}

/**
 * Generates a whole batch of puzzles across every core on the machine.
 *
 * The trick is to file rather than to ask. Asking for one grade at a time means throwing
 * away every puzzle that came out at a different difficulty, and since roughly half of all
 * puzzles come out Gentle and about one in fifty comes out Tricky, that wastes almost
 * everything. Rating each puzzle once and filing it under whatever it turned out to be
 * means the rare grades set the length of the run and the common ones come free.
 *
 * This is an offline job, not something the app runs. It exists so a batch can be built,
 * checked, and shipped inside the APK, which is how Sendoku gets to promise hard puzzles
 * with no server behind it.
 */
public object BatchRun {

    public fun run(request: BatchRequest, onProgress: (BatchResult) -> Unit = {}): BatchResult {
        val started = System.currentTimeMillis()
        val attempts = AtomicLong()
        val unrateable = AtomicLong()
        val done = AtomicBoolean(false)
        val collector = Collector(request.targets)

        val pool = Executors.newFixedThreadPool(request.workers)
        try {
            val jobs = (0 until request.workers).map { worker ->
                Callable {
                    // Each worker owns its stream of seeds, so a run is reproducible for a
                    // given worker count and nothing is shared but the collector.
                    val random = Random(request.seed * 1_000_003L + worker)
                    val maker = GradedGenerator(request.dims, random, TechniqueSolver())
                    while (!done.get()) {
                        if (attempts.incrementAndGet() > request.maxAttempts) {
                            done.set(true)
                            break
                        }
                        val spec = GradeSpec.defaults
                        val rated = maker.next(request.symmetry, digFloor = 0)
                        if (rated == null) {
                            unrateable.incrementAndGet()
                            continue
                        }
                        if (!spec.getValue(rated.grade).accepts(rated.clueCount)) continue
                        if (collector.offer(rated)) done.set(true)
                    }
                }
            }
            pool.invokeAll(jobs)
        } finally {
            pool.shutdownNow()
        }

        val result = BatchResult(
            puzzles = collector.drain(),
            attempts = attempts.get(),
            unrateable = unrateable.get(),
            elapsedMillis = System.currentTimeMillis() - started,
        )
        onProgress(result)
        return result
    }

    /** Fills one bucket per grade, and says when every bucket is full. */
    private class Collector(private val targets: Map<Grade, Int>) {
        private val buckets = HashMap<Grade, MutableList<RatedPuzzle>>()

        @Synchronized
        fun offer(rated: RatedPuzzle): Boolean {
            val wanted = targets[rated.grade] ?: 0
            val bucket = buckets.getOrPut(rated.grade) { ArrayList() }
            if (bucket.size < wanted) bucket.add(rated)
            return targets.all { (grade, target) -> (buckets[grade]?.size ?: 0) >= target }
        }

        @Synchronized
        fun drain(): List<RatedPuzzle> = Grade.entries
            .flatMap { buckets[it].orEmpty() }
            .sortedWith(compareBy({ it.grade.ordinal }, { it.rating }))
    }
}
