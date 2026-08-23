package com.sendoku.engine.catalog

import com.sendoku.engine.Grade
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DailyPuzzleTest {

    /** 2026-08-23 was a Sunday. Epoch day for it, worked out once and pinned. */
    private val sunday = 20688L

    @Test
    fun `the weekday is worked out correctly`() {
        // Epoch day zero, 1970-01-01, was a Thursday, which is index three counting from Monday.
        assertEquals(3, DailyPuzzle.weekdayOf(0))
        assertEquals(4, DailyPuzzle.weekdayOf(1))
        assertEquals(0, DailyPuzzle.weekdayOf(4))
        assertEquals(6, DailyPuzzle.weekdayOf(sunday))
        assertEquals(0, DailyPuzzle.weekdayOf(sunday + 1))
    }

    @Test
    fun `the weekday never goes out of range, even before the epoch`() {
        for (day in -800L..800L) {
            assertTrue(DailyPuzzle.weekdayOf(day) in 0..6, "day $day")
        }
    }

    @Test
    fun `the week ramps from gentle to beyond`() {
        assertEquals(7, DailyPuzzle.rotation.size)
        assertEquals(Grade.GENTLE, DailyPuzzle.rotation.first())
        assertEquals(Grade.BEYOND, DailyPuzzle.rotation.last())
        // Monday is the easiest day of the week and Sunday the hardest.
        assertEquals(Grade.GENTLE, DailyPuzzle.gradeFor(sunday + 1))
        assertEquals(Grade.BEYOND, DailyPuzzle.gradeFor(sunday))
    }

    @Test
    fun `the same day always gives the same puzzle`() {
        for (day in 20_000L..20_100L) {
            assertEquals(DailyPuzzle.gradeFor(day), DailyPuzzle.gradeFor(day))
            assertEquals(DailyPuzzle.indexFor(day, 500), DailyPuzzle.indexFor(day, 500))
        }
    }

    @Test
    fun `the index always lands inside the batch`() {
        for (choices in listOf(1, 2, 7, 100, 500)) {
            for (day in 20_000L..20_200L) {
                assertTrue(DailyPuzzle.indexFor(day, choices) in 0 until choices, "day $day of $choices")
            }
        }
    }

    @Test
    fun `a grade never repeats a puzzle until it has used them all`() {
        // This is the property a hash of the date cannot give. Ten years of Mondays, and
        // every one of them a puzzle the player has not seen.
        val mondays = (0L until 520L).map { sunday + 1 + it * 7 }
        assertTrue(mondays.all { DailyPuzzle.gradeFor(it) == Grade.GENTLE })
        val used = mondays.map { DailyPuzzle.indexFor(it, 500) }
        assertEquals(500, used.take(500).toSet().size, "a puzzle came round twice inside 500 weeks")
        // And once it has been all the way round, it starts again from the beginning.
        assertEquals(used[0], used[500])
    }

    @Test
    fun `a grade that falls on two days a week gets two different puzzles`() {
        // Severe is both Thursday and Friday, and they must not be the same grid.
        val thursday = sunday + 4
        val friday = sunday + 5
        assertEquals(Grade.SEVERE, DailyPuzzle.gradeFor(thursday))
        assertEquals(Grade.SEVERE, DailyPuzzle.gradeFor(friday))
        assertTrue(DailyPuzzle.indexFor(thursday, 500) != DailyPuzzle.indexFor(friday, 500))
    }

    @Test
    fun `consecutive weeks are far apart in the batch`() {
        // Neighbouring entries within a grade have neighbouring ratings, so a stride of one
        // would hand out a run of near identical puzzles.
        for (week in 0L until 50L) {
            val first = DailyPuzzle.indexFor(sunday + 1 + week * 7, 500)
            val next = DailyPuzzle.indexFor(sunday + 1 + (week + 1) * 7, 500)
            assertTrue(kotlin.math.abs(first - next) > 5, "weeks $week and ${week + 1} are next to each other")
        }
    }

    @Test
    fun `the stride shares no factor with the batch size`() {
        for (choices in listOf(3, 7, 100, 250, 500, 512, 999)) {
            val stride = DailyPuzzle.strideFor(choices)
            var a = stride
            var b = choices.toLong()
            while (b != 0L) {
                val t = a % b
                a = b
                b = t
            }
            assertEquals(1L, a, "stride $stride is not coprime with $choices")
        }
    }

    @Test
    fun `a batch of one still works`() {
        assertEquals(0, DailyPuzzle.indexFor(12345L, 1))
    }

    @Test
    fun `an empty batch is refused rather than dividing by zero`() {
        try {
            DailyPuzzle.indexFor(1L, 0)
            error("choosing from nothing should have been refused")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("no puzzles"))
        }
    }
}
