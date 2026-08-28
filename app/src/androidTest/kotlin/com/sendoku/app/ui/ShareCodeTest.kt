package com.sendoku.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.sendoku.app.theme.SendokuTheme
import com.sendoku.engine.Grade
import com.sendoku.engine.catalog.PuzzleCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * Pasting a code somebody sent you.
 *
 * The box is the one place in the app where something arrives from outside the phone, so
 * what is checked here is mostly what it refuses. A code that is nonsense, one with a typo
 * in it, one that arrived cut in half, and one from a newer version are four different
 * problems, and a box that answers all four with the same shrug is a box that gets a bug
 * report saying "it does not work".
 */
class ShareCodeTest {

    @get:Rule
    val compose = createComposeRule()

    private var pasted: String? = null

    private fun show(fault: com.sendoku.engine.catalog.CodeFault? = null, miss: CodeMiss? = null) {
        compose.setContent {
            SendokuTheme {
                HomeScreen(
                    state = HomeState(solvedByGrade = mapOf(Grade.GENTLE to 1), inProgress = null),
                    onPlay = {},
                    onResume = {},
                    onDaily = {},
                    fault = fault,
                    miss = miss,
                    onCode = { pasted = it },
                )
            }
        }
    }

    @Test
    fun aCodeTypedInIsHandedOver() {
        show()
        compose.onNodeWithTag("home:code:field").performTextInput("A-4TQ")
        compose.onNodeWithTag("home:code:play").performClick()
        compose.waitForIdle()
        assertEquals("A-4TQ", pasted)
    }

    @Test
    fun anEmptyBoxDoesNothing() {
        show()
        compose.onNodeWithTag("home:code:play").performClick()
        compose.waitForIdle()
        assertNull("an empty box was sent anyway", pasted)
    }

    @Test
    fun everyKindOfWrongCodeHasItsOwnSentence() {
        // Four faults and two misses, and no two of them may be answered with the same
        // words. A box that says "that did not work" to all of them is a box that earns a
        // bug report saying "it does not work".
        val sentences = mutableListOf<String>()
        compose.setContent {
            SendokuTheme {
                for (fault in com.sendoku.engine.catalog.CodeFault.entries) {
                    sentences += checkNotNull(codeMessage(fault, null))
                }
                for (miss in CodeMiss.entries) {
                    sentences += checkNotNull(codeMessage(null, miss))
                }
            }
        }
        compose.waitForIdle()
        // Six sentences for seven cases. The one deliberate pair is a code that names a
        // puzzle out of range and one that names a puzzle this version has not got, which
        // are the same thing arrived at from two directions.
        assertEquals("two of them share a sentence that should not", 6, sentences.toSet().size)
    }

    @Test
    fun aRealCodeReadsBackAsTheSamePuzzle() {
        // The engine has its own tests for this. What is checked here is that the app is
        // handing the same text to the reader that the player typed, spaces and all.
        show()
        val code = PuzzleCode.forBatch(2048)
        compose.onNodeWithTag("home:code:field").performTextInput(" ${code.lowercase()} ")
        compose.onNodeWithTag("home:code:play").performClick()
        compose.waitForIdle()
        assertEquals(" ${code.lowercase()} ", pasted)
    }

    @Test
    fun theBoxSaysWhatItIsFor() {
        show()
        compose.onNodeWithText("Play a puzzle you were sent").assertIsDisplayed()
    }
}
