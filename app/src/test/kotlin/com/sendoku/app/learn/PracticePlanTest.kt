package com.sendoku.app.learn

import com.sendoku.engine.technique.TechniqueId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** What practice offers next, which is the whole of the review queue. */
class PracticePlanTest {

    private val now = 1_787_000_000_000L
    private val week = PracticePlan.REVIEW_AFTER_MILLIS

    private fun mastered(at: Long) =
        Mastery(attempts = 5, correct = 5, streak = 3, mastered = true, lastPractisedAt = at)

    @Test
    fun `a player who has met nothing is offered nothing`() {
        assertNull(PracticePlan.next(emptyList(), emptyMap(), now))
    }

    @Test
    fun `something met but never practised comes first`() {
        val met = listOf(TechniqueId.NAKED_SINGLE, TechniqueId.X_WING)
        val mastery = mapOf(TechniqueId.NAKED_SINGLE to mastered(now))
        assertEquals(TechniqueId.X_WING, PracticePlan.next(met, mastery, now))
    }

    @Test
    fun `the hardest unmastered one comes next`() {
        val met = listOf(TechniqueId.NAKED_PAIR, TechniqueId.XY_WING, TechniqueId.NAKED_SINGLE)
        val mastery = met.associateWith { Mastery(attempts = 2, correct = 1, streak = 1) }
        assertEquals(TechniqueId.XY_WING, PracticePlan.next(met, mastery, now))
    }

    @Test
    fun `a mastered technique comes back after a week, oldest first`() {
        val met = listOf(TechniqueId.NAKED_SINGLE, TechniqueId.HIDDEN_SINGLE)
        val mastery = mapOf(
            TechniqueId.NAKED_SINGLE to mastered(now - week * 2),
            TechniqueId.HIDDEN_SINGLE to mastered(now - week - 1),
        )
        assertEquals(TechniqueId.NAKED_SINGLE, PracticePlan.next(met, mastery, now))
    }

    @Test
    fun `nothing due still offers something rather than a date`() {
        // "Come back on Thursday" is the kind of thing this app does not say.
        val met = listOf(TechniqueId.NAKED_SINGLE, TechniqueId.HIDDEN_SINGLE)
        val mastery = met.associateWith { mastered(now - 1000) }
        assertEquals(TechniqueId.NAKED_SINGLE, PracticePlan.next(met, mastery, now))
    }

    @Test
    fun `mixed practice leans towards what is not mastered`() {
        val met = listOf(TechniqueId.NAKED_SINGLE, TechniqueId.X_WING)
        val mastery = mapOf(TechniqueId.NAKED_SINGLE to mastered(now))
        val picks = (0 until 40).mapNotNull { PracticePlan.mixed(met, mastery, it) }
        val unmastered = picks.count { it == TechniqueId.X_WING }
        assertTrue("mixed practice ignored what is not mastered: $unmastered of 40", unmastered > picks.size / 2)
    }

    @Test
    fun `practising ahead is allowed and known to be ahead`() {
        val met = listOf(TechniqueId.NAKED_SINGLE)
        assertTrue(PracticePlan.isAhead(TechniqueId.ALS_XZ, met))
        assertTrue(!PracticePlan.isAhead(TechniqueId.NAKED_SINGLE, met))
    }
}
