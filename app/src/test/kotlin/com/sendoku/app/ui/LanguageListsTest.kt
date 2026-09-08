package com.sendoku.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The four lists that decide what languages this app has, checked against each other.
 *
 * Shipping a language means writing the same fact down in four places, and nothing but
 * memory keeps them in step. The four are a values folder full of strings, an entry in the
 * Language enum so the picker offers it, an entry in locales_config.xml so the phone's own
 * per app language screen knows about it, and an entry in resourceConfigurations so it
 * survives packaging.
 *
 * Three of the four fail loudly when they are wrong. The fourth does not: resourceConfigurations
 * is an allowlist, and a translation missing from it is stripped out of the APK at package
 * time with no error anywhere. The picker still offers the language, the phone still lists it,
 * and choosing it does nothing at all. That is how Ukrainian was first shipped: a complete
 * translation, on a phone, in English, with every check green.
 *
 * The enum is not read here, because a JVM test cannot load the generated R class. What can be
 * read is the source, which is the thing a person edits and therefore the thing that drifts.
 */
class LanguageListsTest {

    /** A language tag as a resource folder writes it. */
    private fun folderOf(tag: String) = when (tag) {
        // English is the default, so it has no qualifier and lives in plain values.
        "en" -> "values"

        "zh-Hans" -> "values-b+zh+Hans"

        // The numbering system in a tag is about how digits are drawn, not about which file
        // is read. Resources resolve on the language alone, so ar-u-nu-latn reads values-ar.
        else -> "values-" + tag.substringBefore("-u-")
    }

    /** A language tag as the resourceConfigurations allowlist writes it. */
    private fun configOf(tag: String) = when (tag) {
        "zh-Hans" -> "b+zh+Hans"
        else -> tag.substringBefore("-u-")
    }

    /** Every tag in the Language enum, in the order the picker shows them. */
    private val tags: List<String> by lazy {
        val source = File("src/main/kotlin/com/sendoku/app/ui/Languages.kt")
        assertTrue("Languages.kt is missing", source.isFile)
        val body = source.readText().substringAfter("public enum class Language(").substringBefore("\n}")
        Regex("""\n\s{4}[A-Z_]+\("([^"]*)",""").findAll(body).map { it.groupValues[1] }
            .filter { it.isNotEmpty() }
            .toList()
    }

    private val localesConfig: List<String> by lazy {
        Regex("""android:name="([^"]+)"""")
            .findAll(File("src/main/res/xml/locales_config.xml").readText())
            .map { it.groupValues[1] }
            .toList()
    }

    private val resourceConfigurations: List<String> by lazy {
        val line = File("build.gradle.kts").readLines().first { it.contains("resourceConfigurations +=") }
        Regex(""""([^"]+)"""").findAll(line).map { it.groupValues[1] }.toList()
    }

    @Test
    fun `the guard is reading real lists rather than empty ones`() {
        assertTrue("only ${tags.size} languages were parsed out of the enum", tags.size >= 13)
        assertTrue("locales_config.xml parsed empty", localesConfig.size >= 13)
        assertTrue("resourceConfigurations parsed empty", resourceConfigurations.size >= 13)
    }

    @Test
    fun `every language the picker offers has a folder of strings`() {
        for (tag in tags) {
            val folder = File("src/main/res/${folderOf(tag)}/strings.xml")
            assertTrue("$tag is in the picker but ${folder.path} does not exist", folder.isFile)
        }
    }

    @Test
    fun `every folder of strings is a language the picker offers`() {
        // The other way round, which is the case that leaves a finished translation
        // unreachable: the file is there, nothing offers it, and nothing complains.
        val folders = File("src/main/res").listFiles().orEmpty()
            .filter { it.name.startsWith("values") && File(it, "strings.xml").isFile }
            .map { it.name }
        val offered = tags.map(::folderOf).toSet()
        for (folder in folders) {
            assertTrue("$folder is translated but no Language entry reaches it", folder in offered)
        }
    }

    @Test
    fun `every language the picker offers is in locales_config`() {
        // This is what puts Sendoku in the phone's own per app language screen, and what
        // tells the Play Store which languages the listing may claim.
        assertEquals("locales_config.xml has drifted from the picker", tags, localesConfig)
    }

    @Test
    fun `every language the picker offers survives packaging`() {
        // The quiet one. resourceConfigurations is an allowlist, so a language missing from
        // it is stripped out of the APK and the app falls back to English with no error.
        for (tag in tags) {
            assertTrue(
                "$tag is in the picker but not in resourceConfigurations, so it is stripped from the APK",
                configOf(tag) in resourceConfigurations,
            )
        }
    }

    @Test
    fun `the pseudolocales are still there and are not offered to anybody`() {
        // en-XA doubles every string and ar-XB mirrors the layout. They have to be packaged
        // to be testable, and they must never appear in the picker as languages.
        for (pseudo in listOf("en-rXA", "ar-rXB")) {
            assertTrue("$pseudo was dropped, and it is how layout bugs are found", pseudo in resourceConfigurations)
        }
        assertTrue(
            "a pseudolocale is being offered as a language",
            tags.none {
                it.contains("XA") || it.contains("XB")
            },
        )
    }
}
