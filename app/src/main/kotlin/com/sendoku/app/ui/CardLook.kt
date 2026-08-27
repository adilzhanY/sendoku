package com.sendoku.app.ui

import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.ResourcesCompat
import com.sendoku.app.theme.Sendoku
import com.sendoku.app.theme.SendokuThemes

/**
 * The theme, handed to the canvas that draws the shared card.
 *
 * The card is the one thing in the app drawn outside Compose, so it cannot read the theme the
 * way everything else does. This is the bridge: the same colours the screen is using, as
 * packed ints, and the same typeface, loaded as a real font rather than as a family.
 *
 * Colours come from the theme rather than being chosen here, which is what keeps the card
 * inside the contrast rules the palette already passes. A theme added later gets a card that
 * matches it without anybody remembering this file exists.
 */
@Composable
internal fun rememberCardLook(): ShareCard.Look {
    val context = LocalContext.current
    val themeId = Sendoku.themeId
    val colors = Sendoku.colors
    return remember(themeId, colors) {
        val (regular, bold) = SendokuThemes.fonts(themeId)
        ShareCard.Look(
            background = colors.background.toArgb(),
            board = colors.surface.toArgb(),
            hairline = colors.hairline.toArgb(),
            boxLine = colors.boxLine.toArgb(),
            given = colors.muted.toArgb(),
            entry = colors.given.toArgb(),
            muted = colors.muted.toArgb(),
            accent = colors.accent.toArgb(),
            warn = colors.conflict.toArgb(),
            // A missing font would be a broken build rather than a broken card, but the
            // platform face is a better answer than a crash on somebody's finished game.
            regular = ResourcesCompat.getFont(context, regular) ?: Typeface.SANS_SERIF,
            bold = ResourcesCompat.getFont(context, bold) ?: Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD),
        )
    }
}
