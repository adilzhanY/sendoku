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
 * The mark is the same five by five grid spelling an S that the launcher icon uses, drawn from
 * the same description rather than loaded from a bitmap, so it is crisp at any size and there
 * is no second copy of the logo to keep in step.
 */
public object ShareCard {

    public const val WIDTH: Int = 1080
    public const val HEIGHT: Int = 1350

    /** Deep Field, because a shared card should look like the app people will download. */
    private const val INK = 0xFF0A0E12.toInt()
    private const val TEAL = 0xFF4FE8DA.toInt()
    private const val PAPER = 0xFFE8F0F5.toInt()
    private const val MUTED = 0xFF7D95A5.toInt()
    private const val SLATE = 0xFF2A3540.toInt()
    private const val ROSE = 0xFFFF5C7A.toInt()

    private val MARK = listOf(
        ".####",
        "#....",
        ".###.",
        "....#",
        "####.",
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
    public fun draw(appName: String, title: String, grade: String, lines: List<Line>, grid: Grid?): Bitmap {
        val bitmap = createBitmap(WIDTH, HEIGHT)
        val canvas = Canvas(bitmap)
        canvas.drawColor(INK)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val bold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val plain = Typeface.SANS_SERIF

        drawMark(canvas, paint, left = 84f, top = 84f, side = 116f)

        paint.typeface = bold
        paint.color = PAPER
        paint.textSize = 72f
        canvas.drawText(appName, 232f, 152f, paint)

        paint.typeface = plain
        paint.color = MUTED
        paint.textSize = 34f
        paint.letterSpacing = 0.14f
        canvas.drawText(title.uppercase(), 234f, 200f, paint)
        paint.letterSpacing = 0f

        // The grade, on the right of the same line as the name, so the board can start high.
        paint.typeface = bold
        paint.color = TEAL
        paint.textSize = 62f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(grade, WIDTH - 84f, 168f, paint)
        paint.textAlign = Paint.Align.LEFT

        if (grid != null) drawGrid(canvas, paint, grid, top = 250f, side = 880f)

        val top = HEIGHT - 150f
        val step = (WIDTH - 168f) / lines.size
        for ((index, line) in lines.withIndex()) {
            val centre = 84f + step * index + step / 2
            paint.textAlign = Paint.Align.CENTER

            paint.typeface = bold
            paint.color = if (line.warn) ROSE else PAPER
            paint.textSize = 76f
            canvas.drawText(line.value, centre, top + 36f, paint)

            paint.typeface = plain
            paint.color = MUTED
            paint.textSize = 32f
            paint.letterSpacing = 0.12f
            canvas.drawText(line.label.uppercase(), centre, top + 92f, paint)
            paint.letterSpacing = 0f
            paint.textAlign = Paint.Align.LEFT
        }

        return bitmap
    }

    /**
     * The board, with the player's own digits brighter than the clues.
     *
     * Box rules are drawn heavier than cell rules, the same way they are in the app, because a
     * grid without them is a wall of eighty one numbers.
     */
    private fun drawGrid(canvas: Canvas, paint: Paint, grid: Grid, top: Float, side: Float) {
        val left = (WIDTH - side) / 2
        val cell = side / grid.size

        paint.color = 0xFF121820.toInt()
        canvas.drawRoundRect(RectF(left, top, left + side, top + side), 10f, 10f, paint)

        paint.typeface = Typeface.SANS_SERIF
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = cell * 0.58f
        for (index in grid.digits.indices) {
            val digit = grid.digits[index]
            if (digit == 0) continue
            val column = index % grid.size
            val row = index / grid.size
            paint.color = if (index in grid.given) MUTED else PAPER
            paint.typeface =
                if (index in grid.given) Typeface.SANS_SERIF else Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            canvas.drawText(
                digit.toString(),
                left + column * cell + cell / 2,
                top + row * cell + cell * 0.71f,
                paint,
            )
        }
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.SANS_SERIF

        for (line in 0..grid.size) {
            val heavy = line % grid.boxWidth == 0
            paint.color = if (heavy) SLATE else 0xFF1B2530.toInt()
            paint.strokeWidth = if (heavy) 4f else 2f
            val at = left + line * cell
            canvas.drawLine(at, top, at, top + side, paint)
        }
        for (line in 0..grid.size) {
            val heavy = line % grid.boxHeight == 0
            paint.color = if (heavy) SLATE else 0xFF1B2530.toInt()
            paint.strokeWidth = if (heavy) 4f else 2f
            val at = top + line * cell
            canvas.drawLine(left, at, left + side, at, paint)
        }
    }

    /** The S, as rounded squares on a five by five grid. */
    private fun drawMark(canvas: Canvas, paint: Paint, left: Float, top: Float, side: Float) {
        val gap = side * 0.11f / (MARK.size - 1)
        val cell = (side - gap * (MARK.size - 1)) / MARK.size
        val radius = cell * 0.2f
        for ((row, line) in MARK.withIndex()) {
            for ((column, mark) in line.withIndex()) {
                val x = left + column * (cell + gap)
                val y = top + row * (cell + gap)
                paint.color = if (mark == '#') TEAL else SLATE
                canvas.drawRoundRect(RectF(x, y, x + cell, y + cell), radius, radius, paint)
            }
        }
        paint.color = Color.TRANSPARENT
    }
}
