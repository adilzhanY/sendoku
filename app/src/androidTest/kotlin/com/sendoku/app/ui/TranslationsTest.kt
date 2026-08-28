package com.sendoku.app.ui

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sendoku.app.R
import com.sendoku.app.learn.Curriculum
import com.sendoku.engine.technique.TechniqueId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Every language the app claims to speak, checked against the resources it actually ships.
 *
 * The build already fails on a missing translation, which leaves three ways to ship a broken
 * language and all three are here. A string can be present and empty. A string can keep the
 * words and lose a format argument, which is not a typo but a crash, in the language nobody
 * on this project reads. And a batch can be half done, where the file is full but the words
 * are still English.
 */
@RunWith(AndroidJUnit4::class)
class TranslationsTest {

    private val languages = listOf("en", "ru", "de", "tr")

    /**
     * The app's resources as somebody reading [language] would get them.
     *
     * The whole locale list has to be replaced, not just the first entry. Setting one locale
     * leaves the rest of the phone's list behind it, and Android falls through to the next
     * one it has resources for, which is how this test first came back saying German was a
     * hundred per cent English.
     */
    private fun context(language: String): Context {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocales(LocaleList(Locale.forLanguageTag(language)))
        return base.createConfigurationContext(configuration)
    }

    /** Every string the app declares, by name, read out of the generated R class. */
    private fun everyString(): List<Pair<String, Int>> = R.string::class.java.fields
        .filter { it.type == Int::class.javaPrimitiveType }
        .map { it.name to it.getInt(null) }

    @Test
    fun nothingIsBlankInAnyLanguage() {
        for (language in languages) {
            val resources = context(language)
            for ((name, id) in everyString()) {
                assertTrue("$name is empty in $language", resources.getString(id).isNotBlank())
            }
        }
    }

    @Test
    fun everyTranslationKeepsItsFormatArguments() {
        // The one that would crash rather than merely read badly. A string with a %1$s that
        // loses it, or gains a second one, throws the moment it is formatted.
        val english = context("en")
        val pattern = Regex("%\\d+\\$[sd]")
        for (language in languages - "en") {
            val resources = context(language)
            for ((name, id) in everyString()) {
                val expected = pattern.findAll(english.getString(id)).map { it.value }.toSortedSet()
                val actual = pattern.findAll(resources.getString(id)).map { it.value }.toSortedSet()
                assertEquals("$name has the wrong format arguments in $language", expected, actual)
            }
        }
    }

    @Test
    fun theCourseIsWrittenInEveryLanguage() {
        for (language in languages) {
            val resources = context(language)
            for (lesson in Curriculum.lessons) {
                assertTrue(
                    "${lesson.id} has no title in $language",
                    resources.getString(lesson.title).isNotBlank(),
                )
                assertTrue(
                    "${lesson.id} has no summary in $language",
                    resources.getString(lesson.summary).isNotBlank(),
                )
            }
        }
    }

    @Test
    fun everyTechniqueSpeaksEveryLanguage() {
        for (language in languages) {
            val resources = context(language)
            for (id in TechniqueId.entries) {
                // Every rule has a name in every language, including the cage rules, which
                // the engine can already fire even though no screen can reach them yet.
                assertTrue("$id has no name in $language", resources.getString(TechniqueCopy.nameOf(id)).isNotBlank())
                val look = TechniqueCopy.lookFor(id)
                val because = TechniqueCopy.because(id)
                assertEquals("$id explains half of itself", look == null, because == null)
                if (look == null) continue
                assertTrue("$id has no lookFor in $language", resources.getString(look).isNotBlank())
                assertTrue("$id has no because in $language", resources.getString(because!!).isNotBlank())
            }
        }
    }

    @Test
    fun aLanguageIsNotSecretlyStillEnglish() {
        // What a half finished batch looks like from the outside: the file is full, the lint
        // rule is happy, and every sentence is the English one. Proper names are expected to
        // match, so this is about the sentences.
        val english = context("en")
        val sentences = everyString().filter { (name, _) ->
            name.startsWith("lesson_") || name.startsWith("because_") || name.startsWith("look_")
        }
        assertTrue("no sentences were found to compare", sentences.size > 200)

        for (language in languages - "en") {
            val resources = context(language)
            val same = sentences.count { (_, id) -> resources.getString(id) == english.getString(id) }
            val share = same.toDouble() / sentences.size
            assertTrue(
                "$language leaves ${(share * 100).toInt()} per cent of its sentences in English",
                share < 0.05,
            )
        }
    }
}
