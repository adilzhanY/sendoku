package com.sendoku.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE = "com.sendoku.app"
private const val WAIT_MILLIS = 5_000L

/**
 * Cold start, measured the way a player experiences it: from a tap on the launcher to the
 * first frame with something on it.
 *
 * Run against a real device or a warm emulator, with the app not already running:
 *
 *   ./gradlew :benchmark:connectedBenchmarkAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun coldStartNoProfile() = startup(CompilationMode.None())

    @Test
    fun coldStartWithProfile() = startup(CompilationMode.Partial())

    private fun startup(mode: CompilationMode) = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.COLD,
        compilationMode = mode,
    ) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.res("home:ladder")), WAIT_MILLIS)
    }

    /**
     * The two things a finger does to this app: scroll the ladder, and enter a digit. Both are
     * checked for dropped frames, because a sudoku app that stutters on a tap feels broken in
     * a way no benchmark number conveys.
     */
    @Test
    fun jank() = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.Partial(),
    ) {
        startActivityAndWait()
        val ladder = device.wait(Until.findObject(By.res("home:ladder")), WAIT_MILLIS)
        ladder?.fling(androidx.test.uiautomator.Direction.DOWN)
        ladder?.fling(androidx.test.uiautomator.Direction.UP)

        device.findObject(By.text("Gentle"))?.click()
        device.wait(Until.hasObject(By.res("game:board")), WAIT_MILLIS)
        repeat(6) { index ->
            device.findObject(By.res("game:cell:$index"))?.click()
            device.findObject(By.res("pad:${index % 9 + 1}"))?.click()
            device.waitForIdle()
        }
    }
}
