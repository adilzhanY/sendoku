package com.sendoku.app.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.sendoku.app.StoreShot
import com.sendoku.app.data.CatalogPuzzleSource
import com.sendoku.app.data.DailyDays
import com.sendoku.app.data.FinishedGame
import com.sendoku.app.data.HintLog
import com.sendoku.app.data.Statistics
import com.sendoku.app.game.GameState
import com.sendoku.app.game.Hint
import com.sendoku.app.game.HintEngine
import com.sendoku.app.game.HintLevel
import com.sendoku.app.game.PuzzleOrigin
import com.sendoku.app.game.SolvePath
import com.sendoku.app.learn.CourseProgress
import com.sendoku.app.learn.CourseScreen
import com.sendoku.app.learn.Curriculum
import com.sendoku.app.learn.LessonId
import com.sendoku.app.learn.LessonPlayer
import com.sendoku.app.learn.LessonProgress
import com.sendoku.app.theme.SendokuTheme
import com.sendoku.app.theme.SendokuThemeId
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Grade
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import com.sendoku.engine.catalog.RatedPuzzle
import com.sendoku.engine.technique.TechniqueId
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The screenshots in README.md, drawn rather than tapped out by hand.
 *
 * Same idea as [StoreShotsTest] and a separate class on purpose. The store wants eight
 * pictures at three screen sizes and has rules about what may be in them; the README is a
 * product page and wants a wider set at one size, including the screens that only make sense
 * to somebody reading the source. Sharing one class would mean every store run also
 * rendering the README set and every README change risking the store set.
 *
 *   ./tools/readme-shots.sh
 *
 * It is a test only because that is how you get a Compose renderer on a device. It asserts
 * nothing, and the @StoreShot annotation keeps it out of the ordinary test run.
 */
@StoreShot
class ReadmeShotsTest {

    @get:Rule
    val compose = createComposeRule()

    private val output: File by lazy {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), "readme").also { it.mkdirs() }
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(output, "$name.png").outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    @Composable
    private fun Scene(
        theme: SendokuThemeId = SendokuThemeId.DEEP_FIELD,
        dark: Boolean = true,
        content: @Composable (Modifier) -> Unit,
    ) {
        SendokuTheme(themeId = theme, dark = dark) {
            ReadableWidth { pane -> content(pane) }
        }
    }

    @Composable
    private fun Game(state: GameState) {
        GameScreen(
            state = state,
            onEvent = {},
            onNextPuzzle = {},
            onHome = {},
            onGlossary = { _ -> },
            onSettings = {},
            onPath = {},
            onSpend = { _, _ -> },
        )
    }

    private fun puzzle(grade: Grade, seed: Int): RatedPuzzle {
        val maker = GradedGenerator(Dimensions.CLASSIC, Random(seed))
        var made: RatedPuzzle? = null
        while (made == null || made.grade != grade) made = maker.next(Symmetry.ROTATIONAL)
        return made
    }

    /** A board part solved, with a selection and some notes, so it looks played rather than dealt. */
    private fun midGame(state: GameState, seed: Int, placed: Int, marked: Int = 4): GameState {
        var game = state
        val empties = game.cells.indices.filter { game.cells[it].isEmpty }.shuffled(Random(seed))
        for (index in empties.take(placed)) {
            game = game.select(index).enter(game.solution.atIndex(index))
        }
        val notes = empties.drop(placed)
        game = game.setPencilMode(true)
        for ((offset, index) in notes.take(marked).withIndex()) {
            game = game.select(index).enter(1 + offset).enter(4).enter(if (offset % 2 == 0) 7 else 9)
        }
        return game.setPencilMode(false).select(notes.first())
    }

    /**
     * Walks a puzzle forward with the app's own hint engine until the next step is the
     * technique asked for.
     *
     * A hint screenshot on a fresh board says NAKED SINGLE, which is the opposite of the
     * pitch. Naming the technique rather than a cost also means the picture does not change
     * character when a rule's cost is retuned.
     */
    private fun untilHard(grade: Grade, seed: Int, wanted: TechniqueId): GameState {
        // A puzzle that actually needs the rule, found by asking the rater rather than by
        // hoping. Walking a puzzle that never needs an X-Wing just solves it.
        val maker = GradedGenerator(Dimensions.CLASSIC, Random(seed))
        var made: RatedPuzzle? = null
        while (made == null || made.grade != grade || wanted !in made.usage) made = maker.next(Symmetry.ROTATIONAL)
        var state = GameState.start(made)
        repeat(400) {
            val hint = HintEngine.next(state)
            if (hint !is Hint.Step) return state
            if (hint.deduction.technique == wanted) return state
            val before = state
            state = state.applyHint(hint.deduction)
            if (state == before) return state
        }
        return state
    }

    @Test
    fun home() {
        val left = midGame(GameState.start(puzzle(Grade.SEVERE, seed = 3)), seed = 3, placed = 29, marked = 0)
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
                            grade = Grade.SEVERE,
                            placed = 29 + left.cells.count { it.isGiven },
                            total = 81,
                            elapsed = 18.minutes + 12.seconds,
                            givens = left.cells.joinToString("") { if (it.isGiven) it.digit.toString() else "." },
                            entries = left.cells.joinToString("") {
                                if (!it.isGiven && !it.isEmpty) it.digit.toString() else "."
                            },
                        ),
                        streak = 9,
                    ),
                    onPlay = {},
                    onResume = {},
                    onDaily = {},
                    onKiller = {},
                    modifier = pane,
                )
            }
        }
        shot("home")
    }

    @Test
    fun game() {
        val state = midGame(GameState.start(puzzle(Grade.BEYOND, seed = 91)), seed = 91, placed = 14)
            .tick(11.minutes + 4.seconds)
        compose.setContent { Scene { Game(state) } }
        shot("game")
    }

    @Test
    fun hint() {
        compose.setContent {
            Scene { Game(untilHard(Grade.BEYOND, seed = 7, TechniqueId.X_WING).tick(14.minutes + 30.seconds)) }
        }
        // The toolbar key opens the menu of what to ask for, and the menu's own explain
        // button is what produces the full write-up. Two taps, not one.
        compose.onNodeWithText("Hint", ignoreCase = true).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("hint:menu:explain").performClick()
        compose.waitForIdle()
        // Forward to the last card of the deck. That is the one worth photographing: the
        // cells lit on the board and the argument written out under them. The earlier cards
        // deliberately say less, and a picture of one of those sells nothing.
        repeat(2) {
            compose.onNodeWithTag("hint:more").performClick()
            compose.waitForIdle()
        }
        shot("hint")
    }

    @Test
    fun killer() {
        val dealt = runBlocking { CatalogPuzzleSource.fromResources().killer() }
        checkNotNull(dealt) { "this build ships no Killer batch, so there is nothing to photograph" }
        val start = GameState.start(dealt.puzzle, origin = PuzzleOrigin.KILLER, cages = dealt.cages)
        val state = midGame(start, seed = 5, placed = 34, marked = 3).tick(9.minutes + 26.seconds)
        compose.setContent { Scene { Game(state) } }
        shot("killer")
    }

    @Test
    fun won() {
        val finished = wonGame()
        compose.setContent { Scene { Game(finished) } }
        shot("won")
    }

    /**
     * A Diabolical grid, finished.
     *
     * The clock is wound on before the last digit goes in, because a finished game stops
     * taking ticks and a winning screen reading 0:00 sells nothing.
     */
    private fun wonGame(): GameState {
        var state = GameState.start(puzzle(Grade.DIABOLICAL, seed = 12), catalogIndex = 412)
            .tick(27.minutes + 41.seconds)
        for (index in state.cells.indices) {
            if (state.cells[index].isEmpty) state = state.select(index).enter(state.solution.atIndex(index))
        }
        return state
    }

    /**
     * The picture the app makes of a finished game, drawn by the app rather than screenshotted.
     *
     * It is not a screen, so it does not go through [shot]. It is 1080 by 1350 whatever phone
     * it is made on, which is the point of it.
     */
    @Test
    fun card() {
        var made: GameCard? = null
        compose.setContent {
            SendokuTheme(themeId = SendokuThemeId.DEEP_FIELD, dark = true) {
                made = rememberGameCard(wonGame())
            }
        }
        compose.waitForIdle()
        val card = checkNotNull(made)
        val bitmap = ShareCard.draw(
            appName = card.appName,
            title = card.title,
            grade = card.grade,
            lines = card.lines,
            grid = card.grid,
            look = card.look,
        )
        File(output, "card.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun learn() {
        compose.setContent {
            Scene { pane ->
                CourseScreen(
                    progress = course(finished = 21),
                    onOpen = {},
                    onPractise = {},
                    onBack = {},
                    modifier = pane,
                )
            }
        }
        shot("learn")
    }

    @Test
    fun lesson() {
        val lesson = Curriculum.lessons.first { it.id == LessonId.X_WING }
        compose.setContent {
            Scene { pane ->
                LessonPlayer(lesson = lesson, onFinished = {}, onLeave = {}, startAt = 2, modifier = pane)
            }
        }
        shot("lesson")
    }

    @Test
    fun you() {
        compose.setContent {
            Scene { pane ->
                AccountScreen(
                    statistics = Statistics.of(history(), today = TODAY),
                    course = course(finished = 21),
                    history = history(),
                    onStats = {},
                    onHistory = {},
                    onSettings = {},
                    onAbout = {},
                    modifier = pane,
                )
            }
        }
        shot("you")
    }

    @Test
    fun stats() {
        compose.setContent {
            Scene { pane ->
                StatsScreen(
                    statistics = Statistics.of(history(), today = TODAY),
                    hints = HintLog(
                        byTechnique = mapOf(
                            TechniqueId.X_WING to 6,
                            TechniqueId.XY_WING to 4,
                            TechniqueId.SIMPLE_COLOURING to 2,
                        ),
                        byLevel = mapOf(HintLevel.REGION to 7, HintLevel.CELLS to 3, HintLevel.FULL to 2),
                    ),
                    onBack = {},
                    onReset = {},
                    modifier = pane,
                )
            }
        }
        shot("stats")
    }

    @Test
    fun glossary() {
        compose.setContent { Scene { pane -> GlossaryScreen(onBack = {}, onLesson = {}, modifier = pane) } }
        shot("glossary")
    }

    @Test
    fun path() {
        val path = SolvePath.of(puzzle(Grade.DIABOLICAL, seed = 12).puzzle.givens.copy())
        compose.setContent { Scene { pane -> SolvePathScreen(path = path, onBack = {}, modifier = pane) } }
        shot("path")
    }

    @Test
    fun daily() {
        val solved = (1..23).map { TODAY.minusDays(it.toLong()).toEpochDay() }.toSet() +
            setOf(TODAY.toEpochDay())
        compose.setContent {
            Scene { pane ->
                DailyScreen(
                    today = TODAY,
                    days = DailyDays(solved = solved, attempted = solved),
                    onPlay = {},
                    onBack = {},
                    modifier = pane,
                )
            }
        }
        shot("daily")
    }

    @Test
    fun technique() {
        val supply = listOf(
            TechniqueId.X_WING to 128,
            TechniqueId.XY_WING to 96,
            TechniqueId.SWORDFISH to 41,
            TechniqueId.SIMPLE_COLOURING to 33,
            TechniqueId.UNIQUE_RECTANGLE to 27,
            TechniqueId.XY_CHAIN to 18,
            TechniqueId.ALS_XZ to 9,
        ).map { (technique, count) -> TechniqueSupply(technique, count, hasLesson = true) }
        compose.setContent {
            Scene { pane ->
                ByTechniqueScreen(supply = supply, onPlay = {}, onLearn = {}, onBack = {}, modifier = pane)
            }
        }
        shot("technique")
    }

    /**
     * The same board, once per theme.
     *
     * A set of four is the only honest way to sell a theme. Describing one is worthless, and
     * showing a different screen for each makes them look like four apps. One test each,
     * because the Compose rule takes one setContent per test and no more.
     */
    @Test
    fun themeDeepField() = theme(SendokuThemeId.DEEP_FIELD, dark = true)

    @Test
    fun themeInk() = theme(SendokuThemeId.INK, dark = false)

    @Test
    fun themeZen() = theme(SendokuThemeId.ZEN, dark = true)

    @Test
    fun themeTerminal() = theme(SendokuThemeId.TERMINAL, dark = true)

    private fun theme(id: SendokuThemeId, dark: Boolean) {
        val state = midGame(GameState.start(puzzle(Grade.TRICKY, seed = 4)), seed = 4, placed = 30)
            .tick(6.minutes + 18.seconds)
        compose.setContent { Scene(theme = id, dark = dark) { Game(state) } }
        shot("theme-" + id.name.lowercase())
    }

    /** Twenty one lessons done, which puts the course map mid climb rather than at either end. */
    private fun course(finished: Int) = CourseProgress(
        lessons = Curriculum.lessons.take(finished).associate { it.id to LessonProgress(finished = true) },
        mastery = emptyMap(),
    )

    /** A history that looks like somebody has been playing for a month, because an empty one shows nothing. */
    private fun history(): List<FinishedGame> {
        val day = 24L * 60 * 60 * 1000
        val now = TODAY.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() + 11 * 60 * 60 * 1000
        return buildList {
            repeat(34) { add(finished(Grade.GENTLE, TechniqueId.HIDDEN_SINGLE, 4, now - it * day)) }
            repeat(21) { add(finished(Grade.STEADY, TechniqueId.NAKED_PAIR, 8, now - it * day)) }
            repeat(12) { add(finished(Grade.TRICKY, TechniqueId.X_WING, 14, now - it * day)) }
            repeat(6) { add(finished(Grade.SEVERE, TechniqueId.XY_WING, 22, now - it * day)) }
            repeat(2) { add(finished(Grade.DIABOLICAL, TechniqueId.XY_CHAIN, 38, now - it * day)) }
        }
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

    private companion object {
        /** A fixed day, so the streak and the calendar come out the same on every run. */
        val TODAY: LocalDate = LocalDate.of(2026, 8, 29)
    }
}
