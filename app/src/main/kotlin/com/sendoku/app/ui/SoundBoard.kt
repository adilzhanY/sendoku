package com.sendoku.app.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.sendoku.app.R

/** Every noise the app can make, and nothing else. */
public enum class Sound {
    /** A cell taking the selection. The quietest of them by a long way. */
    TAP,

    /** A digit going in. */
    PLACE,

    /** Notes mode coming on, and going off again, which is the same shape backwards. */
    NOTES_ON,
    NOTES_OFF,

    /** A cell being cleared. */
    ERASE,

    /** A digit the answer does not want. */
    MISTAKE,

    /** The last cell of a finished puzzle. */
    WIN,
}

/**
 * The sounds, chosen rather than found.
 *
 * Every one is a short note from a five note pentatonic set, which is the trick behind why
 * this does not become annoying: there is no pair of notes in that set that clashes, so a
 * fast run of taps comes out as a phrase rather than as noise. They are synthesised sine
 * tones with a fast attack and a quick decay, which is a marimba rather than a bell, and a
 * bell is what makes a phone sound like a slot machine.
 *
 * Two of them earn their loudness and the rest are deliberately under it. Placing a digit is
 * the thing a player did on purpose and gets a clear note; selecting a cell happens forty
 * times a minute and is barely there. The mistake is the one that had to be got right: two
 * low notes a whole tone apart, falling, which reads as "no" without reading as an alarm.
 * Nothing here is a buzzer, because a buzzer is a punishment and this app does not punish.
 *
 * They are generated rather than sampled, so there is no licence to honour, nothing to
 * attribute, and the whole set is a hundred and twenty kilobytes.
 */
public class SoundBoard(context: Context) {

    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                // A game sound, not media. This is what lets it duck under a phone call and
                // keeps it out of the way of whatever music the player has on.
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val ids = buildMap {
        put(Sound.TAP, pool.load(context, R.raw.tap, 1))
        put(Sound.PLACE, pool.load(context, R.raw.place, 1))
        put(Sound.NOTES_ON, pool.load(context, R.raw.notes_on, 1))
        put(Sound.NOTES_OFF, pool.load(context, R.raw.notes_off, 1))
        put(Sound.ERASE, pool.load(context, R.raw.erase, 1))
        put(Sound.MISTAKE, pool.load(context, R.raw.mistake, 1))
        put(Sound.WIN, pool.load(context, R.raw.win, 1))
    }

    /** Volumes, so the mix is a decision rather than whatever the files happened to be. */
    private fun volumeOf(sound: Sound): Float = when (sound) {
        Sound.TAP -> 0.22f
        Sound.PLACE -> 0.55f
        Sound.NOTES_ON, Sound.NOTES_OFF -> 0.40f
        Sound.ERASE -> 0.35f
        Sound.MISTAKE -> 0.50f
        Sound.WIN -> 0.65f
    }

    public fun play(sound: Sound) {
        val id = ids[sound] ?: return
        val volume = volumeOf(sound)
        pool.play(id, volume, volume, 1, 0, 1f)
    }

    public fun release() {
        pool.release()
    }
}

/**
 * One sound board for as long as the screen is on it.
 *
 * A [SoundPool] holds decoded audio and a handful of tracks, so it is built once and let go
 * when the screen does. Building one per tap would be several milliseconds of silence
 * followed by a sound arriving after the thing it was about.
 */
@Composable
public fun rememberSoundBoard(): SoundBoard {
    val context = LocalContext.current
    val board = remember(context) { SoundBoard(context) }
    DisposableEffect(board) { onDispose { board.release() } }
    return board
}
