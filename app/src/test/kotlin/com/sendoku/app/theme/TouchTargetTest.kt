package com.sendoku.app.theme

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nothing tappable is smaller than a fingertip, with one documented exception.
 *
 * A nine by nine grid on a phone gives each cell about forty two density pixels, and no
 * amount of care makes that forty eight: forty eight times nine is four hundred and thirty
 * two, which is wider than the screen. Every guideline that sets the figure carves out dense
 * grids for exactly this reason. Everything that is not a board cell has no such excuse.
 */
class TouchTargetTest {

    @Test
    fun `every theme keeps a full sized touch target`() {
        for (theme in SendokuThemeId.entries) {
            val dimens = SendokuThemes.dimens(theme)
            assertTrue(
                "${theme.displayName} shrank the minimum touch target",
                dimens.minTouchTarget.value >= 48f,
            )
        }
    }

    @Test
    fun `a board cell is as large as the screen allows`() {
        // The narrowest phone Sendoku supports is 320 density pixels wide. After the board's
        // own margins that is about thirty two per cell, which is the real floor.
        val narrowest = 320f
        val margin = SendokuThemes.dimens(SendokuThemeId.DEEP_FIELD).spaceM.value * 2
        val cell = (narrowest - margin) / 9
        assertTrue("a cell would be $cell wide, which is not usable", cell >= 30f)
    }

    @Test
    fun `the gap between keys never eats the target`() {
        for (theme in SendokuThemeId.entries) {
            val dimens = SendokuThemes.dimens(theme)
            assertTrue(
                "${theme.displayName} puts more gap than key between the pad keys",
                dimens.padGap.value < dimens.minTouchTarget.value / 4,
            )
        }
    }
}
