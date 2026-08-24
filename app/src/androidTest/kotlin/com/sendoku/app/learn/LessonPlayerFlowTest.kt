package com.sendoku.app.learn

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sendoku.app.theme.SendokuTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * A lesson, driven the way a finger drives it.
 *
 * The unit tests cover the board at each step. These cover the wiring: that a tap really steps,
 * that a wrong answer is explained rather than counted, and that the step reached is reported
 * so it can be saved.
 */
class LessonPlayerFlowTest {

    @get:Rule
    val compose = createComposeRule()

    private val lesson = Curriculum.byId(LessonId.FIRST_SOLVE_FOUR)

    private fun play(startAt: Int = 0): MutableList<Int> {
        val steps = mutableListOf<Int>()
        compose.setContent {
            SendokuTheme {
                LessonPlayer(
                    lesson = lesson,
                    onFinished = { steps.add(FINISHED) },
                    onLeave = {},
                    startAt = startAt,
                    onStep = { steps.add(it) },
                )
            }
        }
        return steps
    }

    @Test
    fun nextStepsForwardAndBackStepsBack() {
        val steps = play()
        compose.onNodeWithTag("lesson:next").performClick()
        compose.onNodeWithTag("lesson:next").performClick()
        compose.onNodeWithTag("lesson:previous").performClick()

        assertEquals(listOf(1, 2, 1), steps)
    }

    @Test
    fun backIsDeadOnTheFirstStep() {
        play()
        compose.onNodeWithTag("lesson:previous").assertIsDisplayed()
        compose.onNodeWithTag("lesson:previous").performClick()
        // Still on the first step, and nothing crashed.
        compose.onNodeWithTag("lesson:text").assertIsDisplayed()
    }

    @Test
    fun aLessonCanStartWhereItWasLeft() {
        play(startAt = 3)
        compose.onNodeWithText("4 of 6", substring = true).assertIsDisplayed()
    }

    @Test
    fun theWrongAnswerIsExplainedAndTheLessonWaits() {
        val turn = lesson.steps.filterIsInstance<Step.YourTurn>().first()
        val at = lesson.steps.indexOf(turn)
        play(startAt = at)

        val wrong = (1..4).first { it != turn.digit }
        compose.onNodeWithTag("pad:$wrong").performClick()

        // The pad is still there, so the lesson did not move on.
        compose.onNodeWithTag("pad:${turn.digit}").assertIsDisplayed()
        compose.onNodeWithTag("lesson:reveal").assertIsDisplayed()
    }

    @Test
    fun theRightAnswerMovesOn() {
        val turn = lesson.steps.filterIsInstance<Step.YourTurn>().first()
        play(startAt = lesson.steps.indexOf(turn))

        compose.onNodeWithTag("pad:${turn.digit}").performClick()

        // The pad is gone and the way on is back.
        compose.onNodeWithTag("lesson:next").assertIsDisplayed()
    }

    @Test
    fun theLastStepFinishesRatherThanStepping() {
        val steps = play(startAt = lesson.steps.lastIndex)
        compose.onNodeWithTag("lesson:next").performClick()
        assertTrue("the lesson did not report itself finished", FINISHED in steps)
    }

    private companion object {
        /** A sentinel in the same list, so the test can assert on order as well as on fact. */
        const val FINISHED = -1
    }
}
