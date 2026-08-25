package com.sendoku.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.sendoku.app.game.GameState
import com.sendoku.app.game.Hint
import com.sendoku.app.game.HintEngine
import com.sendoku.app.game.HintLevel
import com.sendoku.app.game.logicCells
import com.sendoku.app.game.struckCells
import com.sendoku.app.learn.Curriculum
import com.sendoku.app.learn.LessonId
import com.sendoku.app.learn.LessonPlayer
import com.sendoku.app.theme.Sendoku
import com.sendoku.app.theme.SendokuTheme
import com.sendoku.app.theme.SendokuThemeId
import com.sendoku.app.theme.SendokuThemes
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Symmetry
import com.sendoku.engine.catalog.GradedGenerator
import com.sendoku.engine.catalog.RatedPuzzle
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.abs
import kotlin.random.Random

/**
 * The board, rendered in every theme, compared against a picture of how it should look.
 *
 * Colour contrast is checked by arithmetic elsewhere, and that catches a theme that is
 * unreadable. It cannot catch a theme where the box borders vanished, the selection wash
 * landed on the wrong cell, or a change to the grid painting broke one look and not the
 * others. Only a picture catches that.
 *
 * The reference images are tied to the device that made them: they were captured on API 36,
 * x86_64, and a different API level renders text differently enough to fail. The emulator in
 * CI is pinned to match. When a change to the look is deliberate, delete the reference image
 * for it, run this once to write a new one, and look at the result before committing it.
 */
class ThemeScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    /** How different two pixels can be before they count as different at all. */
    private val perPixelTolerance = 8

    /** How much of the picture may differ, for antialiasing that lands a shade off. */
    private val allowedFraction = 0.002

    private val puzzle: RatedPuzzle by lazy {
        val maker = GradedGenerator(Dimensions.CLASSIC, Random(31_337))
        var made: RatedPuzzle? = null
        while (made == null) made = maker.next(Symmetry.ROTATIONAL)
        made
    }

    /** One board, mid game, with everything a theme can draw showing at once. */
    private fun scene(): GameState {
        var state = GameState.start(puzzle)
        val empties = state.cells.indices.filter { state.cells[it].isEmpty }
        // A correct entry, a repeated digit, some notes, and a selection.
        state = state.select(empties[0]).enter(state.solution.atIndex(empties[0]))
        state = state.setPencilMode(true).select(empties[1]).enter(2).enter(5).enter(9)
        state = state.setPencilMode(false)
        // Anything but the three cells already used, or the clash lands on the notes and the
        // reference images end up with no pencil marks in them at all.
        val clash = empties.drop(3).first { cell ->
            com.sendoku.engine.Geometry.of(Dimensions.CLASSIC).sees(cell, empties[0])
        }
        state = state.select(clash).enter(state.cells[empties[0]].digit)
        return state.select(empties[2])
    }

    // One test per look, because a compose rule will only set its content once and because a
    // failure should name the theme that broke rather than the first one it reached.

    @Test
    fun deepFieldLight() = check(SendokuThemeId.DEEP_FIELD, dark = false)

    @Test
    fun deepFieldDark() = check(SendokuThemeId.DEEP_FIELD, dark = true)

    @Test
    fun inkLight() = check(SendokuThemeId.INK, dark = false)

    @Test
    fun inkDark() = check(SendokuThemeId.INK, dark = true)

    @Test
    fun zenLight() = check(SendokuThemeId.ZEN, dark = false)

    @Test
    fun zenDark() = check(SendokuThemeId.ZEN, dark = true)

    @Test
    fun terminal() = check(SendokuThemeId.TERMINAL, dark = true)

    @Test
    fun hintRegionDeepFieldDark() = checkHint(SendokuThemeId.DEEP_FIELD, dark = true, cells = false)

    @Test
    fun hintCellsDeepFieldDark() = checkHint(SendokuThemeId.DEEP_FIELD, dark = true, cells = true)

    @Test
    fun hintCellsInkLight() = checkHint(SendokuThemeId.INK, dark = false, cells = true)

    @Test
    fun hintCellsTerminal() = checkHint(SendokuThemeId.TERMINAL, dark = true, cells = true)

    /**
     * A board under a hint: the rest of it stepped back, and the house outlined.
     *
     * Two levels rather than four, since the two that draw nothing on the board are already
     * covered by the plain board pictures above. The position is chosen so the hint names a
     * house, because the outline is the part most likely to break and a naked single has no
     * house to draw.
     */
    private fun checkHint(theme: SendokuThemeId, dark: Boolean, cells: Boolean) {
        val name = "hint_" + (if (cells) "cells_" else "region_") + if (SendokuThemes.isFixed(theme)) {
            theme.name.lowercase()
        } else {
            "${theme.name.lowercase()}_${if (dark) "dark" else "light"}"
        }
        val state = hinted()
        val step = HintEngine.next(state, if (cells) HintLevel.CELLS else HintLevel.REGION) as Hint.Step
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 2f, fontScale = 1f)) {
                SendokuTheme(themeId = theme, dark = dark) {
                    Box(
                        Modifier
                            .size(360.dp, 400.dp)
                            .background(Sendoku.colors.background)
                            .padding(12.dp),
                    ) {
                        SudokuBoard(
                            state = state,
                            onSelect = {},
                            hintLogic = if (cells) step.deduction.logicCells() else emptySet(),
                            hintStrike = if (cells) step.deduction.struckCells() else emptySet(),
                            hintHouses = step.deduction.houses,
                            struckMarks = if (cells) step.deduction.eliminations.toSet() else emptySet(),
                            spotlight = true,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        compareOrWrite(name)
    }

    /** A board whose next step is one that argues about a house, with notes to strike. */
    private fun hinted(): GameState {
        val maker = GradedGenerator(Dimensions.CLASSIC, Random(4_242))
        var state: GameState? = null
        while (state == null) {
            val made = maker.next(Symmetry.ROTATIONAL) ?: continue
            val start = GameState.start(made).fillAllMarks()
            val step = HintEngine.next(start) as? Hint.Step ?: continue
            if (step.deduction.houses.isNotEmpty()) state = start
        }
        return state
    }

    @Test
    fun lessonDeepFieldDark() = checkLesson(SendokuThemeId.DEEP_FIELD, dark = true)

    @Test
    fun lessonInkLight() = checkLesson(SendokuThemeId.INK, dark = false)

    @Test
    fun lessonTerminal() = checkLesson(SendokuThemeId.TERMINAL, dark = true)

    /**
     * A lesson, in the looks most likely to break it.
     *
     * Three rather than all seven: Deep Field dark is the default, Ink light is the only warm
     * paper one, and Terminal has no rounding anywhere and so is where a corner radius baked
     * into the course would show up. The board itself is already covered in every look above.
     */
    private fun checkLesson(theme: SendokuThemeId, dark: Boolean) {
        val name = "lesson_" + if (SendokuThemes.isFixed(theme)) {
            theme.name.lowercase()
        } else {
            "${theme.name.lowercase()}_${if (dark) "dark" else "light"}"
        }
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 2f, fontScale = 1f)) {
                SendokuTheme(themeId = theme, dark = dark) {
                    Box(Modifier.size(360.dp, 620.dp)) {
                        LessonPlayer(
                            lesson = Curriculum.byId(LessonId.NAKED_SINGLE),
                            onFinished = {},
                            onLeave = {},
                            startAt = 2,
                        )
                    }
                }
            }
        }
        compareOrWrite(name)
    }

    private fun compareOrWrite(name: String) {
        compose.waitForIdle()
        val actual = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().context
        val golden = runCatching {
            context.assets.open("screenshots/$name.png").use { android.graphics.BitmapFactory.decodeStream(it) }
        }.getOrNull()

        if (golden == null) {
            write(name, actual)
            error("no reference image for $name, one has been written to the device")
        }
        val different = compare(golden, actual)
        if (different > allowedFraction) {
            write(name, actual)
            error(
                "$name differs in ${"%.3f".format(different * 100)} percent of its pixels, " +
                    describe(golden, actual),
            )
        }
    }

    private fun check(theme: SendokuThemeId, dark: Boolean) {
        val name = if (SendokuThemes.isFixed(theme)) {
            theme.name.lowercase()
        } else {
            "${theme.name.lowercase()}_${if (dark) "dark" else "light"}"
        }
        compose.setContent { Scene(theme, dark) }
        compose.waitForIdle()
        val actual = compose.onRoot().captureToImage().asAndroidBitmap()

        val context = InstrumentationRegistry.getInstrumentation().context
        val golden = runCatching {
            context.assets.open("screenshots/$name.png").use { android.graphics.BitmapFactory.decodeStream(it) }
        }.getOrNull()

        if (golden == null) {
            write(name, actual)
            error("no reference image for $name, one has been written to the device")
        }

        val different = compare(golden, actual)
        if (different > allowedFraction) {
            write(name, actual)
            error(
                "$name differs in ${"%.3f".format(different * 100)} percent of its pixels, " +
                    describe(golden, actual),
            )
        }
    }

    /**
     * The scene, pinned to a density of its own.
     *
     * The size below is in density pixels, so without this the picture comes out a different
     * number of real pixels on every device: 945 by 1050 on a 420 dpi phone, something else on
     * a 480 dpi one. The comparison then fails on all one hundred percent of the pixels,
     * which is what it did in CI on every push while passing here.
     *
     * Two is chosen because it is a round number, not because any particular phone uses it.
     * The font scale is pinned for the same reason: a device left at 1.15 would rewrite every
     * reference image the first time somebody ran this on it.
     */
    @Composable
    private fun Scene(theme: SendokuThemeId, dark: Boolean) {
        CompositionLocalProvider(LocalDensity provides Density(density = 2f, fontScale = 1f)) {
            SendokuTheme(themeId = theme, dark = dark) {
                Box(
                    Modifier
                        .size(360.dp, 400.dp)
                        .background(Sendoku.colors.background)
                        .padding(12.dp),
                ) {
                    SudokuBoard(state = scene(), onSelect = {}, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }

    /**
     * How much of the picture is wrong, and enough detail to tell why.
     *
     * A bare "one hundred percent" says nothing, and this test spent a day failing in CI with
     * exactly that. A size mismatch and a colour shift both come out as everything differing,
     * and they need opposite fixes, so the message names which one it was.
     */
    private fun describe(golden: Bitmap, actual: Bitmap): String {
        if (golden.width != actual.width || golden.height != actual.height) {
            return "the sizes differ: reference ${golden.width}x${golden.height}, " +
                "rendered ${actual.width}x${actual.height}"
        }
        var worst = 0
        var total = 0L
        for (x in 0 until golden.width) {
            for (y in 0 until golden.height) {
                val apart = apart(golden.getPixel(x, y), actual.getPixel(x, y))
                if (apart > worst) worst = apart
                total += apart
            }
        }
        val mean = total.toDouble() / (golden.width * golden.height)
        return "same size, worst channel is $worst apart and the mean is ${"%.2f".format(mean)}, " +
            "against a tolerance of $perPixelTolerance"
    }

    private fun apart(a: Int, b: Int): Int = maxOf(
        abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)),
        abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)),
        abs((a and 0xFF) - (b and 0xFF)),
    )

    private fun compare(golden: Bitmap, actual: Bitmap): Double {
        if (golden.width != actual.width || golden.height != actual.height) return 1.0
        var different = 0
        for (x in 0 until golden.width) {
            for (y in 0 until golden.height) {
                if (apart(golden.getPixel(x, y), actual.getPixel(x, y)) > perPixelTolerance) different++
            }
        }
        return different.toDouble() / (golden.width * golden.height)
    }

    /** Writes what was actually rendered somewhere it can be pulled off the device. */
    private fun write(name: String, bitmap: Bitmap) {
        // Internal storage, which always exists. Pull it with:
        //   adb shell run-as com.sendoku.app cat files/screenshots/NAME.png > NAME.png
        val directory = File(
            InstrumentationRegistry.getInstrumentation().targetContext.filesDir,
            "screenshots",
        )
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
