package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sendoku.app.theme.Sendoku

/**
 * Gives a screen wider than a phone some margin instead of stretching everything across it.
 *
 * A sudoku board twice the size is not twice as good, and a number pad a foot wide is worse
 * than one under a thumb. The background still paints the whole window, so the margin reads
 * as part of the app rather than as a letterbox.
 *
 * It lives here rather than inside the app shell so the store screenshots, which render the
 * screens directly, are framed the same way a tablet really frames them.
 */
@Composable
public fun ReadableWidth(modifier: Modifier = Modifier, content: @Composable (Modifier) -> Unit) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(Sendoku.colors.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        val dimens = Sendoku.dimens
        val cap = if (maxWidth > maxHeight) dimens.contentMaxWidthWide else dimens.contentMaxWidth
        content(Modifier.widthIn(max = cap).fillMaxSize())
    }
}
