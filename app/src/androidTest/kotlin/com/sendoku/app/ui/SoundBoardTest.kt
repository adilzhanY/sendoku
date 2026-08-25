package com.sendoku.app.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every sound the app can make loads and plays.
 *
 * There is nothing here about how they sound, which is not a thing a test can hold an opinion
 * about. What it does catch is the whole class of failure that only shows up on a device: a
 * file that did not make it into the build, a format the decoder will not take, or a pool
 * that throws the first time it is asked for a stream.
 */
@RunWith(AndroidJUnit4::class)
class SoundBoardTest {

    @Test
    fun everySoundLoadsAndPlays() {
        val board = SoundBoard(InstrumentationRegistry.getInstrumentation().targetContext)
        try {
            // Loading is asynchronous, so give the pool a moment before asking it to play.
            Thread.sleep(500)
            for (sound in Sound.entries) board.play(sound)
            Thread.sleep(200)
        } finally {
            board.release()
        }
    }
}
