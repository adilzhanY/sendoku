package com.sendoku.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.sendoku.app.theme.SendokuTheme
import com.sendoku.engine.catalog.CatalogReader
import com.sendoku.engine.technique.TechniqueId
import com.sendoku.engine.technique.Techniques
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Asking the batch for a puzzle that turns on one particular technique.
 *
 * The list is built from the shipped batch rather than from a guess, so the test reads the
 * same file the app does. What matters is that a technique with puzzles behind it can be
 * played, one without is shown and cannot, and neither of them is quietly missing from the
 * list: a rule that is not on the page is a rule the player wonders whether the app knows.
 */
class ByTechniqueTest {

    @get:Rule
    val compose = createComposeRule()

    private val supply: List<TechniqueSupply> by lazy {
        val reader = CatalogReader.from(
            checkNotNull(CatalogReader::class.java.getResourceAsStream("/catalog/classic.sdkb")),
        )
        val counts = reader.needing
        Techniques.ladder.map { TechniqueSupply(it.id, counts[it.id] ?: 0, hasLesson = true) }
    }

    private var played: TechniqueId? = null

    private fun show() {
        compose.setContent {
            SendokuTheme {
                ByTechniqueScreen(
                    supply = supply,
                    onPlay = { played = it },
                    onLearn = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun everyTechniqueTheLadderKnowsIsOnThePage() {
        show()
        for (technique in Techniques.ladder) {
            compose.onNodeWithTag("technique:list").performScrollToNode(hasTestTag("technique:${technique.id.name}"))
            compose.onNodeWithTag("technique:${technique.id.name}").assertIsDisplayed()
        }
    }

    @Test
    fun aTechniqueWithPuzzlesBehindItCanBePlayed() {
        val wanted = supply.first { it.count > 0 }
        show()
        compose.onNodeWithTag("technique:list").performScrollToNode(hasTestTag("technique:${wanted.technique.name}"))
        compose.onNodeWithTag("technique:${wanted.technique.name}").performClick()
        compose.waitForIdle()
        assertEquals(wanted.technique, played)
    }

    @Test
    fun aTechniqueWithNothingBehindItCannotBe() {
        val empty = supply.firstOrNull { it.count == 0 }
        if (empty == null) return
        show()
        compose.onNodeWithTag("technique:list").performScrollToNode(hasTestTag("technique:${empty.technique.name}"))
        compose.onNodeWithTag("technique:${empty.technique.name}").performClick()
        compose.waitForIdle()
        assertNull("a technique with no puzzles dealt one anyway", played)
    }

    @Test
    fun theBatchReallyDoesCoverMostOfTheLadder() {
        // If this ever drops sharply, the batch has stopped covering the techniques the
        // course teaches, and half this screen becomes rows that cannot be played.
        val covered = supply.count { it.count > 0 }
        assertTrue("only $covered techniques have puzzles behind them", covered >= 20)
    }
}
