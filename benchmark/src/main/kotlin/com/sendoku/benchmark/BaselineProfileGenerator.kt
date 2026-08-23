package com.sendoku.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE = "com.sendoku.app"
private const val WAIT_MILLIS = 5_000L

/**
 * Writes the list of methods worth compiling ahead of time.
 *
 * Whatever this test touches is what a fresh install runs fast. So it touches the two paths
 * that matter on the first run: opening the app, and starting a puzzle.
 *
 *   ./gradlew :app:generateBaselineProfile
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startupAndPlay() = rule.collect(packageName = PACKAGE) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.res("home:ladder")), WAIT_MILLIS)

        device.findObject(By.text("Gentle"))?.click()
        device.wait(Until.hasObject(By.res("game:board")), WAIT_MILLIS)

        device.findObject(By.res("game:cell:1"))?.click()
        device.findObject(By.res("pad:1"))?.click()
        device.waitForIdle()

        device.pressBack()
        device.waitForIdle()
    }
}
