package com.sendoku.app.ui

import android.app.Activity
import android.app.LocaleManager
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sendoku.app.MainActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Choosing a language has to change the language.
 *
 * This is here because for a long time it did not. The picker went through AppCompat, the app
 * has no AppCompat activity for AppCompat to apply a locale through, and so the radio button
 * moved and nothing else did: not the words on screen, not the system's own record of what
 * this app is set to. Every test that ever checked a translation had set the language with
 * adb rather than by tapping the thing a player taps, so nothing caught it.
 */
@RunWith(AndroidJUnit4::class)
class LanguageChangeTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun followThePhoneAgain() {
        onActivity { Languages.choose(it, Language.SYSTEM) }
    }

    @Test
    fun choosingALanguageIsWhatTheAppIsThenSetTo() {
        onActivity { activity ->
            Languages.choose(activity, Language.JAPANESE)
            assertEquals(Language.JAPANESE, Languages.current(activity))
        }
    }

    @Test
    fun theSystemIsToldToo() {
        // On 13 and later the per app language belongs to the system: it is what the phone's
        // own settings screen shows, and what survives the app being killed. An app that keeps
        // the choice to itself is one that forgets it.
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        onActivity { Languages.choose(it, Language.GERMAN) }
        val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
        assertEquals("the system was never told", "de", locales[0]?.language)
    }

    @Test
    fun followingThePhoneIsTheAbsenceOfAChoice() {
        onActivity { Languages.choose(it, Language.SYSTEM) }
        assertEquals(Language.SYSTEM, Languages.current(context))
    }

    private fun onActivity(block: (Activity) -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { block(it) }
        }
    }
}
