package com.sendoku.app.data

import com.sendoku.engine.Grade
import com.sendoku.engine.technique.TechniqueId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class StatisticsTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val today: LocalDate = LocalDate.of(2026, 8, 23)

    private fun at(date: LocalDate): Long = date.atStartOfDay(zone).plusHours(12).toInstant().toEpochMilli()

    private fun game(
        grade: Grade = Grade.GENTLE,
        solved: Boolean = true,
        elapsed: Duration = 5.minutes,
        on: LocalDate = today,
        rating: Double = 1.5,
        hardest: TechniqueId? = TechniqueId.NAKED_SINGLE,
        hints: Int = 0,
    ) = FinishedGame(
        givens = ".".repeat(81),
        grade = grade,
        rating = rating,
        hardest = hardest,
        elapsed = elapsed,
        hintsUsed = hints,
        mistakes = 0,
        solved = solved,
        finishedAt = at(on),
    )

    private fun stats(games: List<FinishedGame>) = Statistics.of(games, zone, today)

    @Test
    fun `nothing played means nothing to show`() {
        val empty = stats(emptyList())
        assertTrue(empty.isEmpty)
        assertEquals(0, empty.totalSolved)
        assertEquals(0, empty.currentStreak)
        assertEquals(0, empty.longestStreak)
        assertNull(empty.hardestRating)
        assertNull(empty.hardestGrade)
        assertTrue(empty.hardestTechnique.isEmpty())
        assertEquals(Duration.ZERO, empty.totalTime)
        for (grade in Grade.entries) {
            val record = empty.byGrade.getValue(grade)
            assertEquals(0, record.played)
            assertNull(record.best)
            assertNull(record.average)
        }
    }

    @Test
    fun `best and average are per grade and ignore the ones that were lost`() {
        val result = stats(
            listOf(
                game(Grade.SEVERE, elapsed = 10.minutes),
                game(Grade.SEVERE, elapsed = 20.minutes),
                game(Grade.SEVERE, elapsed = 2.minutes, solved = false),
                game(Grade.GENTLE, elapsed = 3.minutes),
            ),
        )

        val severe = result.byGrade.getValue(Grade.SEVERE)
        assertEquals(2, severe.solved)
        assertEquals(1, severe.abandoned)
        assertEquals(3, severe.played)
        assertEquals(10.minutes, severe.best)
        // The two minute loss would have dragged the average down to something flattering.
        assertEquals(15.minutes, severe.average)

        val gentle = result.byGrade.getValue(Grade.GENTLE)
        assertEquals(3.minutes, gentle.best)
        assertEquals(3.minutes, gentle.average)
    }

    @Test
    fun `a grade with nothing but losses has no best time`() {
        val result = stats(listOf(game(Grade.BEYOND, solved = false)))
        val beyond = result.byGrade.getValue(Grade.BEYOND)
        assertEquals(0, beyond.solved)
        assertEquals(1, beyond.abandoned)
        assertNull(beyond.best)
        assertNull(beyond.average)
    }

    @Test
    fun `a streak counts consecutive days`() {
        val result = stats(
            listOf(
                game(on = today),
                game(on = today.minusDays(1)),
                game(on = today.minusDays(2)),
            ),
        )
        assertEquals(3, result.currentStreak)
        assertEquals(3, result.longestStreak)
    }

    @Test
    fun `several puzzles in one day are still one day of a streak`() {
        val result = stats(List(5) { game(on = today) } + game(on = today.minusDays(1)))
        assertEquals(2, result.currentStreak)
    }

    @Test
    fun `today not being played yet does not end a streak`() {
        // Ending it at midnight would punish somebody for going to bed.
        val result = stats(
            listOf(game(on = today.minusDays(1)), game(on = today.minusDays(2))),
        )
        assertEquals(2, result.currentStreak)
    }

    @Test
    fun `a whole day missed does end a streak`() {
        val result = stats(
            listOf(game(on = today.minusDays(2)), game(on = today.minusDays(3))),
        )
        assertEquals(0, result.currentStreak)
        assertEquals(2, result.longestStreak)
    }

    @Test
    fun `the longest streak survives being broken`() {
        val result = stats(
            listOf(
                game(on = today),
                game(on = today.minusDays(5)),
                game(on = today.minusDays(6)),
                game(on = today.minusDays(7)),
                game(on = today.minusDays(8)),
            ),
        )
        assertEquals(1, result.currentStreak)
        assertEquals(4, result.longestStreak)
    }

    @Test
    fun `a lost game does not keep a streak alive`() {
        val result = stats(
            listOf(game(on = today, solved = false), game(on = today.minusDays(1))),
        )
        // Yesterday was solved, today was only attempted, so the streak is yesterday's alone.
        assertEquals(1, result.currentStreak)
    }

    @Test
    fun `the hardest puzzle solved is remembered`() {
        val result = stats(
            listOf(
                game(Grade.GENTLE, rating = 1.5, hardest = TechniqueId.NAKED_SINGLE),
                game(Grade.BEYOND, rating = 7.5, hardest = TechniqueId.ALS_XZ),
                game(Grade.SEVERE, rating = 4.6, hardest = TechniqueId.UNIQUE_RECTANGLE),
                game(Grade.BEYOND, rating = 7.9, hardest = TechniqueId.ALS_XZ, solved = false),
            ),
        )
        // The 7.9 was never finished, so it does not count as something they have done.
        assertEquals(7.5, result.hardestRating!!, 1e-9)
        assertEquals(Grade.BEYOND, result.hardestGrade)
    }

    @Test
    fun `the histogram counts what each puzzle topped out at`() {
        val result = stats(
            listOf(
                game(hardest = TechniqueId.X_WING),
                game(hardest = TechniqueId.X_WING),
                game(hardest = TechniqueId.XY_WING),
                game(hardest = TechniqueId.X_WING, solved = false),
            ),
        )
        assertEquals(2, result.hardestTechnique[TechniqueId.X_WING])
        assertEquals(1, result.hardestTechnique[TechniqueId.XY_WING])
        assertNull(result.hardestTechnique[TechniqueId.ALS_XZ])
    }

    @Test
    fun `totals add up across every grade`() {
        val result = stats(
            listOf(
                game(Grade.GENTLE, elapsed = 4.minutes, hints = 1),
                game(Grade.SEVERE, elapsed = 30.seconds, hints = 2),
                game(Grade.BEYOND, elapsed = 1.minutes, solved = false, hints = 9),
            ),
        )
        assertEquals(2, result.totalSolved)
        assertEquals(4.minutes + 30.seconds, result.totalTime)
        assertEquals(3, result.totalHints)
        assertTrue(!result.isEmpty)
    }

    @Test
    fun `a streak is measured in local days, not in hours`() {
        // Two games nineteen hours apart can be either one day or two, depending on when.
        val late = today.minusDays(1).atStartOfDay(zone).plusHours(23).toInstant().toEpochMilli()
        val early = today.atStartOfDay(zone).plusHours(1).toInstant().toEpochMilli()
        val result = Statistics.of(
            listOf(
                game().copy(finishedAt = late),
                game().copy(finishedAt = early),
            ),
            zone,
            today,
        )
        assertEquals(2, result.currentStreak)
    }

    @Test
    fun `every grade appears, even the ones never played`() {
        val result = stats(listOf(game(Grade.GENTLE)))
        assertEquals(Grade.entries.toSet(), result.byGrade.keys)
    }
}
