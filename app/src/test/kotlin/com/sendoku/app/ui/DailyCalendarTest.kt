package com.sendoku.app.ui

import com.sendoku.app.data.DailyDays
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * The two pieces of the calendar that are arithmetic rather than drawing.
 *
 * The streak in particular has to survive a clock that is wrong, which is the whole reason it
 * counts a set of days rather than keeping a running total.
 */
class DailyCalendarTest {

    private val today = LocalDate.of(2026, 8, 23)
    private val todayEpoch = today.toEpochDay()

    @Test
    fun `no days solved is no streak`() {
        assertEquals(0, dailyStreak(emptySet(), today))
    }

    @Test
    fun `today alone is a streak of one`() {
        assertEquals(1, dailyStreak(setOf(todayEpoch), today))
    }

    @Test
    fun `an unfinished today does not break yesterday's streak`() {
        val solved = setOf(todayEpoch - 1, todayEpoch - 2, todayEpoch - 3)
        assertEquals(3, dailyStreak(solved, today))
    }

    @Test
    fun `a missed day ends the streak`() {
        val solved = setOf(todayEpoch, todayEpoch - 1, todayEpoch - 3, todayEpoch - 4)
        assertEquals(2, dailyStreak(solved, today))
    }

    @Test
    fun `nothing recent is no streak however much was played before`() {
        val solved = (10L..40L).map { todayEpoch - it }.toSet()
        assertEquals(0, dailyStreak(solved, today))
    }

    @Test
    fun `a clock that jumps backwards does not invent or destroy a streak`() {
        val solved = setOf(todayEpoch, todayEpoch - 1, todayEpoch - 2)
        // The device wakes up believing it is two days earlier. The days already solved are
        // still solved, and the count from that day is still right.
        val confused = today.minusDays(2)
        assertEquals(1, dailyStreak(solved, confused))
        // And when the clock is fixed, nothing was lost.
        assertEquals(3, dailyStreak(solved, today))
    }

    @Test
    fun `a clock that jumps forwards does not backfill`() {
        val solved = setOf(todayEpoch, todayEpoch - 1)
        val leapt = today.plusDays(30)
        assertEquals(0, dailyStreak(solved, leapt))
    }

    @Test
    fun `solved beats attempted, and the future is not playable`() {
        val days = DailyDays(solved = setOf(todayEpoch - 1), attempted = setOf(todayEpoch - 2))
        assertEquals(DayMark.SOLVED, markFor(today.minusDays(1), today, days))
        assertEquals(DayMark.ATTEMPTED, markFor(today.minusDays(2), today, days))
        assertEquals(DayMark.UNPLAYED, markFor(today.minusDays(3), today, days))
        assertEquals(DayMark.UNPLAYED, markFor(today, today, days))
        assertEquals(DayMark.FUTURE, markFor(today.plusDays(1), today, days))
    }
}
