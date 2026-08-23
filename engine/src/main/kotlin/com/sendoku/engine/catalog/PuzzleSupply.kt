package com.sendoku.engine.catalog

import com.sendoku.engine.Grade
import kotlin.random.Random

/** Where a puzzle came from. The app cares, because a shipped puzzle is a rated puzzle. */
public sealed interface Supplied {

    public val puzzle: RatedPuzzle

    /** Straight out of the batch, never played on this device. */
    public data class FromCatalog(val index: Int, override val puzzle: RatedPuzzle) : Supplied

    /** Made on the phone, because the batch had nothing left at this grade. */
    public data class GeneratedLive(override val puzzle: RatedPuzzle) : Supplied

    /**
     * A batch puzzle the player has already finished, handed back because live generation
     * could not produce one in time. The last resort, and never silent: the app should say
     * so rather than pretend the puzzle is new.
     */
    public data class Recycled(val index: Int, override val puzzle: RatedPuzzle) : Supplied
}

/**
 * Hands out the next puzzle of a given grade, and never comes back empty.
 *
 * Five hundred puzzles a grade is a lot, but somebody will finish them, and the day they do
 * the app must not simply stop. So there are three tiers. Take an unplayed one from the
 * batch. If there are none left, make one on the phone, which costs a second or two at the
 * hard grades but is exact. If even that fails, give back one they have already solved and
 * be honest about it.
 *
 * A player who has exhausted five hundred Diabolical puzzles has earned a wait. What they
 * have not earned is an empty screen.
 */
public class PuzzleSupply(
    private val reader: CatalogReader,
    private val generator: GradedGenerator,
    /**
     * How hard to try before recycling. The rare grades turn up about once in fifty, so a
     * few thousand attempts is a second or two of phone time and almost always enough.
     */
    private val liveAttempts: Int = 4_000,
) {

    /**
     * Takes a puzzle of [grade] that is not in [played].
     *
     * [played] holds catalog indices the device has already finished. It belongs to the app,
     * which is the only thing that knows what this player has seen.
     */
    public fun take(grade: Grade, played: Set<Int> = emptySet(), random: Random = Random.Default): Supplied {
        val all = reader.indicesOf(grade)
        val fresh = all.filter { it !in played }
        if (fresh.isNotEmpty()) {
            val index = fresh[random.nextInt(fresh.size)]
            return Supplied.FromCatalog(index, reader.puzzleAt(index))
        }

        val made = generator.generate(grade, attempts = liveAttempts)
        if (made != null) return Supplied.GeneratedLive(made)

        if (all.isEmpty()) {
            error("no ${grade.displayName} puzzle in the batch and none could be made")
        }
        val index = all[random.nextInt(all.size)]
        return Supplied.Recycled(index, reader.puzzleAt(index))
    }

    /** How many unplayed puzzles of [grade] the batch still holds. */
    public fun remaining(grade: Grade, played: Set<Int> = emptySet()): Int =
        reader.indicesOf(grade).count { it !in played }
}
