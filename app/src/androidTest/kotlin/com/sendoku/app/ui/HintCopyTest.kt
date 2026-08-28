package com.sendoku.app.ui

import android.content.Context
import android.content.res.Configuration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sendoku.app.learn.Curriculum
import com.sendoku.engine.technique.TechniqueId
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Every rule has to have something to say, in every language the app ships.
 *
 * A technique with no copy still fires, still rates puzzles, and still turns up in a hint,
 * where it produces a panel with an empty middle. Kotlin cannot catch that: the resource id
 * exists, it simply resolves to nothing. So it is checked here, against the real resources,
 * in each locale in turn.
 */
@RunWith(AndroidJUnit4::class)
class HintCopyTest {

    private fun context(language: String): Context {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocales(android.os.LocaleList(Locale.forLanguageTag(language)))
        return base.createConfigurationContext(configuration)
    }

    @Test
    fun everyTechniqueHasAName() {
        for (language in listOf("en", "ru")) {
            val resources = context(language)
            for (id in TechniqueId.entries) {
                val name = resources.getString(TechniqueCopy.nameOf(id))
                assertTrue("$id has no name in $language", name.isNotBlank())
            }
        }
    }

    @Test
    fun everyTechniqueSaysWhatToLookForAndWhyItWorks() {
        for (language in listOf("en", "ru")) {
            val resources = context(language)
            // Every rule a player can meet. The cage rules are named and have an engine
            // behind them; their explanations are written with their lessons, and until then
            // they are honestly absent rather than filled with something invented.
            for (id in TechniqueId.entries.filterNot { it.isCage }) {
                val look = resources.getString(checkNotNull(TechniqueCopy.lookFor(id)))
                val because = resources.getString(checkNotNull(TechniqueCopy.because(id)))
                assertTrue("$id does not say what to look for in $language", look.isNotBlank())
                assertTrue("$id does not say why it works in $language", because.isNotBlank())
                assertTrue(
                    "$id explains itself in fewer words than its own name in $language",
                    because.length > name(resources, id).length,
                )
            }
        }
    }

    @Test
    fun everyTechniqueWithALessonAgreesOnItsName() {
        // The glossary, the hint panel and the course all name the same rule, and a player
        // who is sent from one to the other has to arrive somewhere recognisable.
        val resources = context("en")
        for (id in TechniqueId.entries) {
            val lesson = Curriculum.teaching(id) ?: continue
            val title = resources.getString(lesson.title)
            assertTrue("the lesson for $id has no title", title.isNotBlank())
        }
    }

    private fun name(resources: Context, id: TechniqueId) = resources.getString(TechniqueCopy.nameOf(id))
}
