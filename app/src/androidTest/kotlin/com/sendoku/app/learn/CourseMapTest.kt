package com.sendoku.app.learn

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.sendoku.app.theme.SendokuTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * The course, on one screen.
 *
 * The page used to be a card per lesson, which is forty two cards and about ten screens of
 * scrolling, and it always opened at lesson one. What is checked here is what the map has to
 * keep true: every stage is on the page, the lesson you are up to is offered without hunting
 * for it, and folding a stage open still reaches the lessons inside it, because a map that
 * loses a lesson has lost the course.
 */
class CourseMapTest {

    @get:Rule
    val compose = createComposeRule()

    /** Twelve lessons in, standing at the start of the fourth stage. */
    private val partWay = CourseProgress(
        lessons = Curriculum.lessons.take(12).associate { it.id to LessonProgress(step = 0, finished = true) },
    )

    private val allDone = CourseProgress(
        lessons = Curriculum.lessons.associate { it.id to LessonProgress(step = 0, finished = true) },
    )

    private fun show(progress: CourseProgress, onOpen: (LessonId) -> Unit = {}, onPractise: () -> Unit = {}) {
        compose.setContent {
            SendokuTheme {
                CourseScreen(progress = progress, onOpen = onOpen, onPractise = onPractise, onBack = null)
            }
        }
    }

    @Test
    fun everyStageIsOnTheMap() {
        show(partWay)
        for (stage in Stage.entries) {
            compose.onNodeWithTag("course:stage:${stage.name}").performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun theLessonYouAreUpToIsOfferedWithoutHunting() {
        var opened: LessonId? = null
        show(partWay, onOpen = { opened = it })
        compose.onNodeWithTag("course:next").performClick()
        compose.waitForIdle()
        assertEquals(partWay.next().id, opened)
    }

    @Test
    fun aStageFoldsOpenIntoItsLessons() {
        var opened: LessonId? = null
        show(partWay, onOpen = { opened = it })

        compose.onNodeWithTag("course:${LessonId.NAKED_PAIR.name}").assertDoesNotExist()
        compose.onNodeWithTag("course:stage:GROUPS").performScrollTo().performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("course:${LessonId.NAKED_PAIR.name}").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(LessonId.NAKED_PAIR, opened)
    }

    @Test
    fun onlyOneStageIsOpenAtATime() {
        // Two open at once and the page is back to being a list of everything, which is the
        // thing the map replaced.
        show(partWay)
        compose.onNodeWithTag("course:stage:GROUPS").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("course:stage:WINGS").performScrollTo().performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("course:${LessonId.NAKED_PAIR.name}").assertDoesNotExist()
        compose.onNodeWithTag("course:${LessonId.XY_WING.name}").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun withNothingLeftToLearnThereIsNoCard() {
        show(allDone)
        compose.onNodeWithTag("course:next").assertDoesNotExist()
        compose.onNodeWithTag("course:practise").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun practiceIsOnThePage() {
        var practised = 0
        show(partWay, onPractise = { practised++ })
        compose.onNodeWithTag("course:practise").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(1, practised)
    }
}
