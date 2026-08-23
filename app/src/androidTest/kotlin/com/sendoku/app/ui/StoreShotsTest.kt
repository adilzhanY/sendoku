package com.sendoku.app.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.sendoku.app.StoreShot
import com.sendoku.app.data.Appearance
import com.sendoku.app.data.FinishedGame
import com.sendoku.app.data.Statistics
import com.sendoku.app.data.ThemeMode
import com.sendoku.app.game.GameSettings
import com.sendoku.app.game.GameState
import com.sendoku.app.game.Hint
import com.sendoku.app.game.HintEngine
import com.sendoku.app.theme.SendokuTheme
import com.sendoku.app.theme.SendokuThemeId
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import com.sendoku.engine.catalog.RatedPuzzle
import com.sendoku.engine.technique.TechniqueId
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The store screenshots, drawn rather than tapped out by hand.
 *
 * Play wants eight pictures at three screen sizes, and every one of them has to show the app
 * in a state worth showing: a hard puzzle mid solve, a hint mid explanation, the ladder with
 * some of it climbed. Driving the real app with taps to reach those states is slow, and it
 * produces a slightly different picture every time. Rendering the same screens with a state
 * written down here produces the same picture every run, on any screen size.
 *
 *   ./tools/store-shots.sh
 *
 * It is a test only because that is how you get a Compose renderer on a device. It asserts
 * nothing, and it is excluded from the ordinary test run.
 */
@StoreShot
class StoreShotsTest {

    @get:Rule
    val compose = createComposeRule()

    private val output: File by lazy {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), "store").also { it.mkdirs() }
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(output, "$name.png").outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    /** The same frame the app puts around a screen, so a tablet shot is framed like a tablet. */
    @Composable
    private fun Scene(dark: Boolean = true, content: @Composable (Modifier) -> Unit) {
        SendokuTheme(themeId = SendokuThemeId.DEEP_FIELD, dark = dark) {
            ReadableWidth { pane -> content(pane) }
        }
    }

    private fun puzzle(grade: Grade, seed: Int): RatedPuzzle {
        val maker = GradedGenerator(Dimensions.CLASSIC, Random(seed))
        var made: RatedPuzzle? = null
        while (made == null || made.grade != grade) made = maker.next(Symmetry.ROTATIONAL)
        return made
    }

    /**
     * A board part solved, with a selection and some notes, so it looks played rather than dealt.
     *
     * The filled cells are scattered rather than taken in index order. Filling the first N
     * empties solves the top three rows and leaves the rest untouched, which no player has
     * ever done and which looks exactly as manufactured as it is.
     */
    private fun midGame(grade: Grade, seed: Int, placed: Int): GameState {
        var state = GameState.start(puzzle(grade, seed))
        val empties = state.cells.indices.filter { state.cells[it].isEmpty }.shuffled(Random(seed))
        for (index in empties.take(placed)) {
            state = state.select(index).enter(state.solution.atIndex(index))
        }
        val notes = empties.drop(placed)
        state = state.setPencilMode(true)
        for ((offset, index) in notes.take(4).withIndex()) {
            state = state.select(index).enter(1 + offset).enter(4).enter(if (offset % 2 == 0) 7 else 9)
        }
        return state.setPencilMode(false).select(notes.first()).tick(11.minutes + 4.seconds)
    }

    @Test
    fun home() {
        compose.setContent {
            Scene { pane ->
                HomeScreen(
                    state = HomeState(
                        solvedByGrade = mapOf(
                            Grade.GENTLE to 34,
                            Grade.STEADY to 21,
                            Grade.TRICKY to 12,
                            Grade.SEVERE to 6,
                            Grade.DIABOLICAL to 2,
                        ),
                        inProgress = InProgressSummary(
                            grade = Grade.DIABOLICAL,
                            placed = 41,
                            total = 81,
                            elapsed = 18.minutes + 12.seconds,
                        ),
                    ),
                    onPlay = {},
                    onResume = {},
                    onDaily = {},
                    onSettings = {},
                    onStats = {},
                    modifier = pane,
                )
            }
        }
        shot("2-home")
    }

    @Test
    fun beyond() {
        compose.setContent {
            Scene { pane ->
                GameScreen(
                    state = midGame(Grade.BEYOND, seed = 91, placed = 14),
                    onEvent = {},
                    onNextPuzzle = {},
                    onHome = {},
                    onGlossary = {},
                    modifier = pane,
                )
            }
        }
        shot("3-beyond")
    }

    /**
     * Solves the board with the app's own hint engine until it reaches a position where the
     * cheapest thing that works is genuinely hard.
     *
     * A hint screenshot taken on a fresh board says NAKED SINGLE, which is the opposite of the
     * pitch. This walks the puzzle forward to the first place a wing or a chain is needed, so
     * the picture shows the app doing the thing nobody else does.
     */
    private fun untilHard(grade: Grade, seed: Int, minimumCost: Double): GameState {
        var state = GameState.start(puzzle(grade, seed))
        repeat(400) {
            val hint = HintEngine.next(state)
            if (hint !is Hint.Step) return state
            if (hint.deduction.technique.cost >= minimumCost) return state
            val before = state
            state = state.applyHint(hint.deduction)
            if (state == before) return state
        }
        return state
    }

    @Test
    fun hint() {
        compose.setContent {
            Scene { pane ->
                GameScreen(
                    state = untilHard(Grade.BEYOND, seed = 7, minimumCost = 4.2)
                        .tick(14.minutes + 30.seconds),
                    onEvent = {},
                    onNextPuzzle = {},
                    onHome = {},
                    onGlossary = {},
                    modifier = pane,
                )
            }
        }
        // Open the hint, then walk it up to the full explanation. The panel's own buttons,
        // not the toolbar: tapping the toolbar again just asks the same question over.
        compose.onNodeWithText("Hint", ignoreCase = true).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Show me where", ignoreCase = true).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Explain it", ignoreCase = true).performClick()
        shot("1-hint")
    }

    @Test
    fun light() {
        compose.setContent {
            Scene(dark = false) { pane ->
                GameScreen(
                    state = midGame(Grade.TRICKY, seed = 4, placed = 30),
                    onEvent = {},
                    onNextPuzzle = {},
                    onHome = {},
                    onGlossary = {},
                    modifier = pane,
                )
            }
        }
        shot("5-light")
    }

    @Test
    fun glossary() {
        compose.setContent { Scene { pane -> GlossaryScreen(onBack = {}, modifier = pane) } }
        shot("4-glossary")
    }

    @Test
    fun stats() {
        compose.setContent {
            Scene { pane -> StatsScreen(statistics = history(), onBack = {}, onReset = {}, modifier = pane) }
        }
        shot("6-stats")
    }

    @Test
    fun settings() {
        compose.setContent {
            Scene { pane ->
                SettingsScreen(
                    settings = GameSettings(),
                    appearance = Appearance(theme = SendokuThemeId.DEEP_FIELD, mode = ThemeMode.DARK),
                    onChange = {},
                    onAppearanceChange = {},
                    onBack = {},
                    onAbout = {},
                    modifier = pane,
                )
            }
        }
        shot("7-settings")
    }

    @Test
    fun solved() {
        var state = GameState.start(puzzle(Grade.DIABOLICAL, seed = 12))
        for (index in state.cells.indices) {
            if (state.cells[index].isEmpty) state = state.select(index).enter(state.solution.atIndex(index))
        }
        val finished = state.tick(27.minutes + 41.seconds)
        compose.setContent {
            Scene { pane ->
                GameScreen(
                    state = finished,
                    onEvent = {},
                    onNextPuzzle = {},
                    onHome = {},
                    onGlossary = {},
                    modifier = pane,
                )
            }
        }
        shot("8-solved")
    }

    /** A history that looks like somebody has been playing for a month, because an empty one shows nothing. */
    private fun history(): Statistics {
        val day = 24L * 60 * 60 * 1000
        val now = 1_780_000_000_000L
        val games = buildList {
            repeat(34) { add(finished(Grade.GENTLE, TechniqueId.HIDDEN_SINGLE, 4, now - it * day)) }
            repeat(21) { add(finished(Grade.STEADY, TechniqueId.NAKED_PAIR, 8, now - it * day)) }
            repeat(12) { add(finished(Grade.TRICKY, TechniqueId.X_WING, 14, now - it * day)) }
            repeat(6) { add(finished(Grade.SEVERE, TechniqueId.XY_WING, 22, now - it * day)) }
            repeat(2) { add(finished(Grade.DIABOLICAL, TechniqueId.XY_CHAIN, 38, now - it * day)) }
        }
        return Statistics.of(games)
    }

    private fun finished(grade: Grade, hardest: TechniqueId, minutes: Int, at: Long) = FinishedGame(
        givens = ".".repeat(81),
        grade = grade,
        rating = hardest.cost,
        hardest = hardest,
        elapsed = minutes.minutes,
        hintsUsed = 0,
        mistakes = 0,
        solved = true,
        finishedAt = at,
    )
}
