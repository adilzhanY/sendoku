package com.sendoku.app.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.sendoku.app.R

/**
 * The digits, and only the digits, in a font the app carries itself.
 *
 * Samsung and Xiaomi let a user replace the system font, and plenty do. On a sudoku board
 * that is not a cosmetic change: a narrow one, or a seven with a bar, or a one that looks
 * like a seven, and the grid stops being readable at a glance. So the digits are pinned.
 *
 * The file holds ten digits and a colon and nothing else, which is six kilobytes rather
 * than the three hundred a full weight of Inter costs. Prose stays on the platform font,
 * where a replacement is a preference rather than a problem, and where the user's own
 * choice of font should win.
 *
 * Because it holds nothing else, this family must never be given anything but digits and
 * colons. Anything else renders as a blank box.
 */
public val DigitFont: FontFamily = FontFamily(
    Font(R.font.inter_digits_regular, FontWeight.Normal),
    Font(R.font.inter_digits_semibold, FontWeight.SemiBold),
)

/**
 * Every text style the app is allowed to use.
 *
 * Sizes here are defaults. The board scales its own digits to the cell, because a grid that
 * fits a small phone and a tablet cannot use one fixed size, so [gridGiven], [gridEntry]
 * and [pencilMark] are given a size to be overridden rather than obeyed.
 */
@Immutable
public data class SendokuType(
    /** The one big number on a screen: a percentage, a time, a grade. */
    val display: TextStyle,

    /** Screen and section headings. */
    val title: TextStyle,

    /** Ordinary prose. Hint explanations live here, so it has to be comfortable to read. */
    val body: TextStyle,

    /** Buttons and list rows. */
    val label: TextStyle,

    /** Small upper case labels, the quiet signposts of the dark theme. */
    val overline: TextStyle,

    /** The word under a tool icon. Smaller than a label, and never upper case. */
    val toolLabel: TextStyle,

    /** The word above a number in the header strip. */
    val statLabel: TextStyle,

    /** The number itself. */
    val statValue: TextStyle,

    /** A clue that came with the puzzle. */
    val gridGiven: TextStyle,

    /** A digit the player entered. */
    val gridEntry: TextStyle,

    /** A pencil mark. The smallest thing in the app, and the first thing to fail a squint. */
    val pencilMark: TextStyle,

    /** A digit on the number pad. */
    val padDigit: TextStyle,

    /** The count of how many of that digit are left. */
    val padCount: TextStyle,

    /** The clock. Tabular so it stops jittering as the seconds turn over. */
    val timer: TextStyle,
)

/** Inter for the digits, the platform sans for everything else. */
public val DefaultType: SendokuType = SendokuType(
    display = TextStyle(
        fontSize = 44.sp,
        lineHeight = 48.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.02).em,
    ),
    title = TextStyle(
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.01).em,
    ),
    body = TextStyle(
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
    ),
    label = TextStyle(
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
    ),
    overline = TextStyle(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.14.em,
    ),
    toolLabel = TextStyle(
        fontSize = 12.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Medium,
    ),
    statLabel = TextStyle(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
    ),
    statValue = TextStyle(
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    gridGiven = TextStyle(
        fontFamily = DigitFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
    ),
    gridEntry = TextStyle(
        fontFamily = DigitFont,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
    ),
    pencilMark = TextStyle(
        fontFamily = DigitFont,
        fontWeight = FontWeight.Normal,
        fontSize = 8.sp,
    ),
    padDigit = TextStyle(
        fontFamily = DigitFont,
        fontWeight = FontWeight.Normal,
        // Bigger than it was, because the card that used to say "this is a key" is gone and
        // the digit has to say it instead.
        fontSize = 30.sp,
    ),
    padCount = TextStyle(
        fontFamily = DigitFont,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
    ),
    timer = TextStyle(
        fontFamily = DigitFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        fontFeatureSettings = "tnum",
    ),
)
