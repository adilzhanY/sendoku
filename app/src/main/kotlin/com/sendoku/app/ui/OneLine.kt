package com.sendoku.app.ui

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * A label on one line, at whatever size that takes.
 *
 * Every row of furniture in the app divides the width of the screen between three, four or
 * five words: the stats above the board, the tools below it, the buttons on a panel. At the
 * ordinary font scale they all fit. At two hundred percent, in German, none of them do, and
 * what happens then is the part worth deciding rather than leaving to the layout.
 *
 * Wrapping is the default and it is the worst of the three: a word breaks in the middle, so
 * "Notizen" becomes "Notize" over "n" and "Schließen" becomes "Schlie" over "ßen", which
 * reads as a broken app rather than a large one. Cutting the word short is worse still,
 * because "Stimmt etwas nicht?" truncated to "Stimmt etwas" is a different sentence.
 *
 * So the text shrinks instead, down to [min] if that is what it takes. Small and whole is
 * the one option of the three that is still the thing it says.
 */
@Composable
internal fun OneLine(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    min: TextUnit = 7.sp,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color, textAlign = TextAlign.Center),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = min, maxFontSize = style.fontSize, stepSize = 0.5.sp),
    )
}
