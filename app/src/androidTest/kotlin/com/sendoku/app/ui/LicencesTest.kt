package com.sendoku.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import com.sendoku.app.theme.SendokuTheme
import org.junit.Rule
import org.junit.Test

/**
 * The licences screen, and the one obligation it carries.
 *
 * The four typefaces are under the SIL Open Font License, which asks for its own text to
 * travel with the fonts rather than be named and left on a website. The text is an asset, so
 * nothing but a running app proves it is still in the build and still reachable.
 */
class LicencesTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theFontLicenceTravelsWithTheApp() {
        compose.setContent { SendokuTheme { LicencesScreen(onBack = {}) } }
        compose.onNodeWithTag("licences:list").performScrollToNode(hasTestTag("licences:ofl"))
        compose.onNodeWithTag("licences:ofl").assertIsDisplayed()
    }
}
