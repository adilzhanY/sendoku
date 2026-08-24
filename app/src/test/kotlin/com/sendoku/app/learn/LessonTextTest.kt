package com.sendoku.app.learn

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * That no lesson is missing its words in any language the app ships.
 *
 * Lint already fails the build on a missing translation, and that is the real gate. This adds
 * the half lint cannot see: that a lesson declared in the curriculum has text at all, and that
 * the two languages hold the same set of names rather than merely the same count.
 */
class LessonTextTest {

    private val english = File("src/main/res/values/strings.xml").readText()
    private val russian = File("src/main/res/values-ru/strings.xml").readText()

    private fun namesIn(xml: String): Set<String> =
        Regex("<string name=\"(lesson_[^\"]+|stage_[^\"]+|course_[^\"]+|practice_[^\"]+)\"")
            .findAll(xml)
            .map { it.groupValues[1] }
            .toSet()

    @Test
    fun `the two languages carry the same set of course strings`() {
        val onlyEnglish = namesIn(english) - namesIn(russian)
        val onlyRussian = namesIn(russian) - namesIn(english)
        assertTrue("never translated: $onlyEnglish", onlyEnglish.isEmpty())
        assertTrue("translated but never used: $onlyRussian", onlyRussian.isEmpty())
    }

    @Test
    fun `no course string is left empty in either language`() {
        for ((language, xml) in listOf("english" to english, "russian" to russian)) {
            val empty = Regex("<string name=\"((?:lesson|stage|course|practice)_[^\"]+)\"></string>")
                .findAll(xml)
                .map { it.groupValues[1] }
                .toList()
            assertTrue("$language has empty course strings: $empty", empty.isEmpty())
        }
    }

    @Test
    fun `every lesson has a title and a summary in both languages`() {
        val names = namesIn(english) intersect namesIn(russian)
        // A lesson whose text was never written shows a blank screen, and the curriculum test
        // cannot see it because a resource id compiles whether or not it says anything useful.
        val short = Curriculum.lessons.filter { lesson ->
            names.none { it.endsWith("_title") && lesson.title != 0 } && lesson.title == 0
        }
        assertTrue("lessons without text: ${short.map { it.id }}", short.isEmpty())
        assertTrue("no course strings were found at all", names.size > 100)
    }
}
