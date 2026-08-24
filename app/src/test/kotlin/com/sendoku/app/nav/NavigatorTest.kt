package com.sendoku.app.nav

import androidx.compose.runtime.saveable.SaverScope
import com.sendoku.engine.Grade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The back stack, and the rule that decides whether the bottom bar is on screen.
 *
 * The bar is the part worth pinning down. It must be there on the three roots and gone
 * everywhere else, because a bar under a puzzle is a way to lose a game by mistake.
 */
class NavigatorTest {

    @Test
    fun `home is a root and shows the bar`() {
        val navigator = Navigator()
        assertTrue(navigator.atRoot)
        assertFalse(navigator.canGoBack)
    }

    @Test
    fun `a puzzle is not a root`() {
        val navigator = Navigator()
        navigator.go(Destination.Play(Grade.GENTLE))
        assertFalse("the bar was drawn under a puzzle", navigator.atRoot)
    }

    @Test
    fun `every tab is a root`() {
        for (tab in Navigator.TABS) {
            val navigator = Navigator()
            navigator.switchTo(tab)
            assertTrue("$tab did not show the bar", navigator.atRoot)
        }
    }

    @Test
    fun `switching tabs leaves nothing behind it`() {
        val navigator = Navigator()
        navigator.go(Destination.Play(Grade.SEVERE))
        navigator.go(Destination.Settings)

        navigator.switchTo(Destination.Course)

        assertEquals(Destination.Course, navigator.current)
        assertFalse("back from a tab unwound the old stack", navigator.canGoBack)
    }

    @Test
    fun `a stack survives being written down and read back`() {
        val navigator = Navigator()
        navigator.switchTo(Destination.Account)
        navigator.go(Destination.Stats)

        val scope = SaverScope { true }
        val saved = with(Navigator.Saver) { requireNotNull(scope.save(navigator)) }
        val restored = requireNotNull(Navigator.Saver.restore(saved))

        assertEquals(Destination.Stats, restored.current)
        assertTrue(restored.canGoBack)
        restored.back()
        assertEquals(Destination.Account, restored.current)
    }
}
