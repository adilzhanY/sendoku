package com.sendoku.app.data

import com.sendoku.app.game.PuzzleOrigin
import com.sendoku.engine.Grade
import com.sendoku.engine.technique.TechniqueId
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration

/** What a player has done at one grade. */
public data class GradeRecord(
    val grade: Grade,
    val solved: Int,
    val abandoned: Int,
    val best: Duration?,
    val average: Duration?,
) {
    val played: Int get() = solved + abandoned
}

/**
 * Everything the statistics screen shows, worked out from the finished games.
 *
 * Computed rather than stored. A running total in a column is a second source of truth that
 * drifts the first time a row is deleted or a migration goes sideways, and there will never
 * be enough finished games here for the difference to be measurable.
 */
public data class Statistics(
    val byGrade: Map<Grade, GradeRecord>,
    val currentStreak: Int,
    val longestStreak: Int,
    val hardestRating: Double?,
    val hardestGrade: Grade?,
    /** How many puzzles topped out at each technique. */
    val hardestTechnique: Map<TechniqueId, Int>,
    val totalSolved: Int,

    /** Every game that reached an end, won or lost. Solved on its own is a lonely number. */
    val gamesPlayed: Int,
    val totalTime: Duration,
    val totalHints: Int,
    /** Solved with no hints, no mistakes and no notes written. */
    val cleanSolves: Int = 0,
    /**
     * Killers solved, counted apart from the ladder.
     *
     * A Killer is graded on the same scale, but it is a different game: counting a Steady
     * Killer among the Steady puzzles beaten would put a number on the ladder that nobody
     * climbed. It is still a game finished, so it is still in the totals.
     */
    val killerSolved: Int = 0,
) {
    val isEmpty: Boolean get() = totalSolved == 0 && byGrade.values.all { it.played == 0 }

    public companion object {

        public fun of(
            games: List<FinishedGame>,
            zone: ZoneId = ZoneId.systemDefault(),
            today: LocalDate = LocalDate.now(zone),
        ): Statistics {
            val solved = games.filter { it.solved }
            val days = solved.map { it.finishedAt.toLocalDate(zone) }.toSortedSet()

            return Statistics(
                // The ladder only. A Killer is graded on the same scale and is not a rung
                // of it, so it is counted on its own rather than mixed in here.
                byGrade = Grade.entries.associateWith { grade ->
                    record(grade, games.filter { it.origin != PuzzleOrigin.KILLER })
                },
                currentStreak = currentStreak(days, today),
                longestStreak = longestStreak(days),
                hardestRating = solved.maxOfOrNull { it.rating },
                hardestGrade = solved.maxByOrNull { it.rating }?.grade,
                hardestTechnique = solved.mapNotNull { it.hardest }.groupingBy { it }.eachCount(),
                totalSolved = solved.size,
                cleanSolves = solved.count { it.isClean },
                killerSolved = solved.count { it.origin == PuzzleOrigin.KILLER },
                gamesPlayed = games.size,
                totalTime = solved.fold(Duration.ZERO) { sum, game -> sum + game.elapsed },
                totalHints = solved.sumOf { it.hintsUsed },
            )
        }

        private fun record(grade: Grade, games: List<FinishedGame>): GradeRecord {
            val atGrade = games.filter { it.grade == grade }
            val won = atGrade.filter { it.solved }
            return GradeRecord(
                grade = grade,
                solved = won.size,
                abandoned = atGrade.size - won.size,
                best = won.minByOrNull { it.elapsed }?.elapsed,
                // Only from puzzles that were actually finished. Averaging in the ones that
                // beat the player would make a hard grade look fast.
                average = if (won.isEmpty()) {
                    null
                } else {
                    won.fold(Duration.ZERO) { sum, game -> sum + game.elapsed } / won.size
                },
            )
        }

        /**
         * Days in a row up to today, counting yesterday as still alive.
         *
         * A streak that dies at midnight punishes somebody for going to bed, so today not
         * being played yet does not end it. It ends when a whole day passes with nothing.
         */
        private fun currentStreak(days: Set<LocalDate>, today: LocalDate): Int {
            if (days.isEmpty()) return 0
            val start = when {
                today in days -> today
                today.minusDays(1) in days -> today.minusDays(1)
                else -> return 0
            }
            var count = 0
            var day = start
            while (day in days) {
                count++
                day = day.minusDays(1)
            }
            return count
        }

        private fun longestStreak(days: Set<LocalDate>): Int {
            if (days.isEmpty()) return 0
            var longest = 1
            var run = 1
            var previous: LocalDate? = null
            for (day in days.sorted()) {
                val last = previous
                if (last != null) {
                    run = if (last.plusDays(1) == day) run + 1 else 1
                }
                if (run > longest) longest = run
                previous = day
            }
            return longest
        }

        private fun Long.toLocalDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
    }
}
