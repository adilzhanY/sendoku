package com.sendoku.app.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.sendoku.app.R

/**
 * The four faces the app carries, one per theme.
 *
 * Two reasons for bundling type at all, and the first is not decoration. Samsung and Xiaomi
 * let a user replace the system font, and plenty do. On a sudoku board that is not a
 * cosmetic change: a narrow one, or a seven with a bar, or a one that looks like a seven,
 * and the grid stops being readable at a glance. A bundled face pins the digits.
 *
 * The second is that a theme is meant to be a whole look. Terminal has been calling itself
 * Terminal while borrowing whatever proportional font the phone happened to have.
 *
 * Each family is two static weights cut down to the 159 characters the app's own strings
 * can produce across English, Russian, German and Turkish. Whole, the four of them are 941
 * kilobytes; cut down they are 113, which is what makes this affordable at all. They are
 * built by tools/subset-fonts.py, which derives that character set from the shipped
 * strings rather than from a list somebody has to remember to update.
 *
 * Weights between the two shipped are resolved to the nearest, which is Compose's own rule.
 */
public val InterFont: FontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

public val PtSerifFont: FontFamily = FontFamily(
    Font(R.font.pt_serif_regular, FontWeight.Normal),
    Font(R.font.pt_serif_bold, FontWeight.Bold),
)

public val ManropeFont: FontFamily = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
)

public val JetBrainsMonoFont: FontFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
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

/**
 * The same scale, in another face.
 *
 * Every style, including the board and the clock, because a theme whose grid is set in one
 * family and whose buttons are set in another is not a theme, it is a mistake.
 */
public fun SendokuType.inFace(family: FontFamily): SendokuType = copy(
    display = display.copy(fontFamily = family),
    title = title.copy(fontFamily = family),
    body = body.copy(fontFamily = family),
    label = label.copy(fontFamily = family),
    overline = overline.copy(fontFamily = family),
    toolLabel = toolLabel.copy(fontFamily = family),
    statLabel = statLabel.copy(fontFamily = family),
    statValue = statValue.copy(fontFamily = family),
    gridGiven = gridGiven.copy(fontFamily = family),
    gridEntry = gridEntry.copy(fontFamily = family),
    pencilMark = pencilMark.copy(fontFamily = family),
    padDigit = padDigit.copy(fontFamily = family),
    padCount = padCount.copy(fontFamily = family),
    timer = timer.copy(fontFamily = family),
)

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
        lineBreak = LineBreak.Heading,
    ),
    body = TextStyle(
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
        // Japanese writes without spaces, so where a line may break is decided by rules
        // rather than by gaps: 禁則処理, which forbids a line starting with a full stop or a
        // closing bracket, or ending with an opening one. Paragraph is the preset that asks
        // for those rules strictly, and it improves the wrapping of the long lesson
        // paragraphs in every other language too. Heading does the same for short headings,
        // where it also keeps a Japanese phrase together rather than breaking it anywhere.
        lineBreak = LineBreak.Paragraph,
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
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
    ),
    gridEntry = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
    ),
    pencilMark = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 8.sp,
    ),
    padDigit = TextStyle(
        fontWeight = FontWeight.Normal,
        // Bigger than it was, because the card that used to say "this is a key" is gone and
        // the digit has to say it instead.
        fontSize = 30.sp,
    ),
    padCount = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
    ),
    timer = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        fontFeatureSettings = "tnum",
    ),
)
