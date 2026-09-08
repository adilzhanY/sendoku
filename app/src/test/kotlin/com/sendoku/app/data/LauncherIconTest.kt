package com.sendoku.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * One icon in the app drawer, and it opens the game.
 *
 * LeakCanary registers a launcher activity of its own, so a debug install used to put two
 * Sendoku shaped entries in the drawer: the game, and a Leaks one that opens LeakCanary's
 * list. Two consequences, and the second is the one that wasted time. Somebody opening the
 * app by hand has to know which of two icons is the game. And a launch resolved by intent
 * filter rather than by name picks whichever the system lists first, which is how
 * `adb shell monkey -c android.intent.category.LAUNCHER` opened LeakCanary instead.
 *
 * `app/src/debug/AndroidManifest.xml` disables the alias. This is what says it stayed
 * disabled, because a LeakCanary upgrade that renames the alias would silently put the
 * second icon back and nothing else would notice.
 */
class LauncherIconTest {

    /** The debug manifest as the merger produced it, which is the only one that knows. */
    private val merged: File? = sequenceOf(
        "build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml",
        "build/intermediates/merged_manifest/debug/AndroidManifest.xml",
    ).map { File(it) }.firstOrNull { it.isFile }

    @Test
    fun `the debug build overrides leakcanary's launcher icon`() {
        // Checked in the source too, not only in the merged output, so this fails on a
        // checkout that has never been built rather than passing on air.
        val source = File("src/debug/AndroidManifest.xml")
        assertTrue("app/src/debug/AndroidManifest.xml is gone, so the Leaks icon is back", source.isFile)
        val text = source.readText()
        assertTrue("the alias is no longer named", text.contains("LeakLauncherActivity"))
        assertTrue("the alias is no longer disabled", text.contains("android:enabled=\"false\""))
    }

    @Test
    fun `only sendoku's own activity is in the launcher`() {
        if (merged == null) {
            println("MANIFEST no merged debug manifest on disk yet, skipping the launcher check")
            return
        }
        val text = merged.readText()

        // Every block that claims a launcher entry, whichever element declares it.
        val launchers = Regex(
            """<(activity|activity-alias)\b[^>]*android:name="([^"]+)"(.*?)</\1>""",
            RegexOption.DOT_MATCHES_ALL,
        )
            .findAll(text)
            .filter { it.groupValues[3].contains("android.intent.category.LAUNCHER") }
            .map { it.groupValues[2] }
            .toList()

        assertEquals("the drawer would show ${launchers.size} icons: $launchers", 1, launchers.size)
        assertTrue(
            "the one launcher entry is not MainActivity: ${launchers.first()}",
            launchers.first().endsWith("MainActivity"),
        )
    }

    @Test
    fun `leakcanary is still in the debug build, only its icon is gone`() {
        // The library is what catches a leaked activity, and the point of this was the drawer,
        // not the tool. If it ever gets dropped, that should be a decision rather than a side
        // effect of tidying an icon away.
        val build = File("build.gradle.kts").readText()
        assertTrue(
            "leakcanary was removed from the debug build",
            build.contains("debugImplementation(libs.leakcanary)"),
        )
        if (merged == null) return
        assertTrue(
            "leakcanary is no longer in the merged debug manifest",
            merged.readText().contains("leakcanary"),
        )
    }
}
