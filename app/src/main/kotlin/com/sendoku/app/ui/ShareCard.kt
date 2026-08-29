package com.sendoku.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.createBitmap

/**
 * The result of one game, as a picture worth sending to somebody.
 *
 * Drawn on a canvas rather than captured from the screen. A screenshot is whatever resolution
 * the phone happens to be, carries the status bar and the navigation bar, and looks like a
 * screenshot. This is 1080 by 1350, the shape every messaging app and social site crops least,
 * and it is drawn at that size rather than scaled up to it, so the digits are sharp on a
 * desktop monitor as well as a phone.
 *
 * The board is on it, and it is the point. A row of numbers says somebody finished a puzzle;
 * the grid they finished says which one, and the digits they put in it are visibly theirs,
 * darker than the clues they were given. That is the thing a person wants to show, and no
 * amount of typography about hints and mistakes replaces it.
 *
 * There is nothing on here about the app being free, carrying no advertisements and tracking
 * nobody. All three are true and none of them belongs on somebody's photograph.
 *
 * The mark is the same one the launcher icon wears, a sudoku box with one cell lit, drawn from
 * the same description rather than loaded from a bitmap, so it is crisp at any size and there
 * is no second copy of the logo to keep in step.
 *
 * It wears whatever the player is wearing. The card used to have Deep Field written into it
 * as six constants, so somebody who had spent a week in Ink and Paper shared a picture of an
 * app they do not use. Now the palette and the typeface both come in from the theme, and the
 * only thing that stays fixed is the mark, because that is the logo rather than the look.
 */
public object ShareCard {

    public const val WIDTH: Int = 1080
    public const val HEIGHT: Int = 1350

    /** The mark's own two colours, the same in every theme. See the note above. */
    private const val MARK_ON = 0xFF4FE8DA.toInt()
    private const val MARK_OFF = 0xFF2A3540.toInt()

    /**
     * The theme, in the only terms a canvas understands.
     *
     * Colours are packed ints and the faces are real typefaces, because [android.graphics] has
     * never heard of a Compose colour or a font family. Everything here comes from the theme
     * the player is in, which is what makes the card a picture of their app.
     */
    public data class Look(
        val background: Int,
        val board: Int,
        val hairline: Int,
        val boxLine: Int,
        val given: Int,
        val entry: Int,
        val muted: Int,
        val accent: Int,
        val warn: Int,
        val regular: Typeface,
        val bold: Typeface,
        /**
         * Whether the reader reads right to left.
         *
         * A canvas mirrors nothing by itself. The words inside a line are laid out correctly
         * either way, because Android does that per string, but where those lines sit on the
         * card is decided here and nowhere else.
         */
        val rightToLeft: Boolean = false,
    )

    private val MARK = listOf(
        "...",
        ".#.",
        "...",
    )

    /** One line of the stats block: what it is, and what it says. */
    public data class Line(val label: String, val value: String, val warn: Boolean = false)

    /**
     * The finished grid, as it was left.
     *
     * [digits] is every cell in reading order, and [given] says which of them the puzzle came
     * with. Anything not given is drawn brighter, because that is the half the player did.
     */
    public data class Grid(
        val size: Int,
        val boxWidth: Int,
        val boxHeight: Int,
        val digits: List<Int>,
        val given: Set<Int>,
    )

    /**
     * Draws the card.
     *
     * [title] is what happened, [grade] the difficulty, and [lines] the numbers. Everything is
     * handed in as text already formatted and translated, because this file must not know how
     * to say "3 of 3" in Russian.
     */
    public fun draw(
        appName: String,
        title: String,
        grade: String,
        lines: List<Line>,
        grid: Grid?,
        look: Look,
    ): Bitmap {
        val bitmap = createBitmap(WIDTH, HEIGHT)
        val canvas = Canvas(bitmap)
        canvas.drawColor(look.background)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val bold = look.bold
        val plain = look.regular

        // The mark leads, so it sits on the side the reader starts from.
        val markLeft = if (look.rightToLeft) WIDTH - 84f - 116f else 84f
        val textEdge = if (look.rightToLeft) markLeft - 32f else 232f
        drawMark(canvas, paint, left = markLeft, top = 84f, side = 116f)

        paint.typeface = bold
        paint.color = look.entry
        paint.textSize = 72f
        paint.textAlign = if (look.rightToLeft) Paint.Align.RIGHT else Paint.Align.LEFT
        canvas.drawText(appName, textEdge, 152f, paint)
        val nameWidth = paint.measureText(appName)

        paint.typeface = plain
        paint.color = look.muted
        paint.textSize = 34f
        paint.letterSpacing = 0.14f
        canvas.drawText(title, if (look.rightToLeft) textEdge else 234f, 200f, paint)
        paint.letterSpacing = 0f
        paint.textAlign = Paint.Align.LEFT

        // The grade, on the right of the same line as the name, so the board can start high.
        // It gets whatever the name has left, and shrinks rather than running into it: a
        // German grade in a monospace face is more than twice the width of an English one.
        paint.typeface = bold
        paint.color = look.accent
        paint.textAlign = if (look.rightToLeft) Paint.Align.LEFT else Paint.Align.RIGHT
        fit(paint, grade, 62f, WIDTH - 84f - (232f + nameWidth) - 32f)
        canvas.drawText(grade, if (look.rightToLeft) 84f else WIDTH - 84f, 168f, paint)
        paint.textAlign = Paint.Align.LEFT

        if (grid != null) drawGrid(canvas, paint, grid, top = 250f, side = 880f, look = look)

        val top = HEIGHT - 150f
        val step = (WIDTH - 168f) / lines.size
        // Read from the other side, so the first thing named is the first thing seen.
        val ordered = if (look.rightToLeft) lines.reversed() else lines
        for ((index, line) in ordered.withIndex()) {
            val centre = 84f + step * index + step / 2
            paint.textAlign = Paint.Align.CENTER

            paint.typeface = bold
            paint.color = if (line.warn) look.warn else look.entry
            fit(paint, line.value, 76f, step - 56f)
            canvas.drawText(line.value, centre, top + 36f, paint)

            paint.typeface = plain
            paint.color = look.muted
            paint.letterSpacing = 0.12f
            fit(paint, line.label, 32f, step - 40f)
            canvas.drawText(line.label, centre, top + 92f, paint)
            paint.letterSpacing = 0f
            paint.textAlign = Paint.Align.LEFT
        }

        return bitmap
    }

    /**
     * Sets the text size, and takes it down until the text fits the width it has.
     *
     * A card is drawn once at a fixed size, so nothing here reflows: whatever is written has
     * to fit where it is put. Three stats share the width of the card and a monospace face is
     * half again as wide as a proportional one, so "1 of 3" in Terminal reached its
     * neighbour. Shrinking is the only move a canvas has, and it never grows the text: the
     * size passed in is the size a short word gets.
     */
    private fun fit(paint: Paint, text: String, size: Float, width: Float): Float {
        paint.textSize = size
        var current = size
        while (current > 12f && paint.measureText(text) > width) {
            current -= 1f
            paint.textSize = current
        }
        return current
    }

    /**
     * The board, with the player's own digits brighter than the clues.
     *
     * Box rules are drawn heavier than cell rules, the same way they are in the app, because a
     * grid without them is a wall of eighty one numbers.
     */
    private fun drawGrid(canvas: Canvas, paint: Paint, grid: Grid, top: Float, side: Float, look: Look) {
        val left = (WIDTH - side) / 2
        val cell = side / grid.size

        paint.color = look.board
        canvas.drawRoundRect(RectF(left, top, left + side, top + side), 10f, 10f, paint)

        paint.typeface = look.regular
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = cell * 0.58f
        for (index in grid.digits.indices) {
            val digit = grid.digits[index]
            if (digit == 0) continue
            val column = index % grid.size
            val row = index / grid.size
            paint.color = if (index in grid.given) look.given else look.entry
            paint.typeface = if (index in grid.given) look.regular else look.bold
            canvas.drawText(
                digit.toString(),
                left + column * cell + cell / 2,
                top + row * cell + cell * 0.71f,
                paint,
            )
        }
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = look.regular

        for (line in 0..grid.size) {
            val heavy = line % grid.boxWidth == 0
            paint.color = if (heavy) look.boxLine else look.hairline
            paint.strokeWidth = if (heavy) 4f else 2f
            val at = left + line * cell
            canvas.drawLine(at, top, at, top + side, paint)
        }
        for (line in 0..grid.size) {
            val heavy = line % grid.boxHeight == 0
            paint.color = if (heavy) look.boxLine else look.hairline
            paint.strokeWidth = if (heavy) 4f else 2f
            val at = top + line * cell
            canvas.drawLine(left, at, left + side, at, paint)
        }
    }

    /** The box, as nine rounded squares with one of them lit. */
    private fun drawMark(canvas: Canvas, paint: Paint, left: Float, top: Float, side: Float) {
        val gap = side * 0.10f / (MARK.size - 1)
        val cell = (side - gap * (MARK.size - 1)) / MARK.size
        val radius = cell * 0.25f
        for ((row, line) in MARK.withIndex()) {
            for ((column, mark) in line.withIndex()) {
                val x = left + column * (cell + gap)
                val y = top + row * (cell + gap)
                paint.color = if (mark == '#') MARK_ON else MARK_OFF
                canvas.drawRoundRect(RectF(x, y, x + cell, y + cell), radius, radius, paint)
            }
        }
        paint.color = Color.TRANSPARENT
    }
}
