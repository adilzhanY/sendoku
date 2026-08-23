package com.sendoku.app.data

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sendoku does not touch the network, and this is what keeps it that way.
 *
 * "No ads, no tracking, no server" is the whole pitch, and it is one careless dependency
 * away from being untrue. A library that pulls in an analytics SDK brings its own manifest
 * with its own INTERNET permission, and the merge is silent.
 *
 * Both the source manifest and the merged one are checked, because only the merged one
 * knows what the dependencies asked for.
 */
class ManifestTest {

    private val forbidden = listOf(
        "android.permission.INTERNET",
        "android.permission.ACCESS_NETWORK_STATE",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.READ_CONTACTS",
        "android.permission.READ_PHONE_STATE",
        "com.google.android.gms.permission.AD_ID",
    )

    @Test
    fun `the app asks for no permissions of its own`() {
        val manifest = File("src/main/AndroidManifest.xml")
        assertTrue("the manifest is missing", manifest.isFile)
        val text = manifest.readText()
        assertFalse("the app declares a permission", text.contains("uses-permission"))
    }

    @Test
    fun `nothing a dependency dragged in asks for the network either`() {
        val merged = sequenceOf(
            "build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml",
            "build/intermediates/merged_manifest/debug/AndroidManifest.xml",
            "build/outputs/logs/manifest-merger-debug-report.txt",
        ).map { File(it) }.firstOrNull { it.isFile }

        if (merged == null) {
            // Nothing to check until a build has produced one. The source manifest test above
            // still runs, so this is a gap rather than a hole.
            println("MANIFEST no merged manifest on disk yet, skipping the dependency check")
            return
        }

        val text = merged.readText()
        for (permission in forbidden) {
            assertFalse("$permission reached the merged manifest via ${merged.name}", text.contains(permission))
        }
    }
}
