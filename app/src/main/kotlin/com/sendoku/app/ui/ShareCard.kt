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
     * Draws the card.
     *
     * [title] is what happened, [grade] the difficulty, and [lines] the numbers. Everything is
     * handed in as text already formatted and translated, because this file must not know how
     * to say "3 of 3" in Russian.
     */
    public fun draw(appName: String, title: String, grade: String, lines: List<Line>, footer: String): Bitmap {
        val bitmap = createBitmap(WIDTH, HEIGHT)
        val canvas = Canvas(bitmap)
        canvas.drawColor(INK)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val bold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val plain = Typeface.SANS_SERIF

        drawMark(canvas, paint, left = 96f, top = 110f, side = 190f)

        paint.typeface = bold
        paint.color = PAPER
        paint.textSize = 86f
        canvas.drawText(appName, 330f, 205f, paint)

        paint.typeface = plain
        paint.color = MUTED
        paint.textSize = 38f
        paint.letterSpacing = 0.14f
        canvas.drawText(footer.uppercase(), 332f, 268f, paint)
        paint.letterSpacing = 0f

        // The headline: what happened, then the grade under it in the accent colour, because
        // the grade is the thing worth bragging about and the app's whole pitch.
        paint.typeface = bold
        paint.color = PAPER
        paint.textSize = 76f
        canvas.drawText(title, 96f, 470f, paint)

        paint.color = TEAL
        paint.textSize = 130f
        canvas.drawText(grade, 96f, 610f, paint)

        // Spread across what is left rather than stacked under the grade. The first draft left
        // a fifth of the card empty at the bottom, which reads as a picture that failed to
        // load rather than as space.
        val top = 790f
        val step = (HEIGHT - 150f - top) / lines.size
        var y = top
        for (line in lines) {
            paint.typeface = plain
            paint.color = MUTED
            paint.textSize = 46f
            canvas.drawText(line.label, 96f, y, paint)

            paint.typeface = bold
            paint.color = if (line.warn) TEAL else PAPER
            paint.textSize = 62f
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(line.value, WIDTH - 96f, y + 6f, paint)
            paint.textAlign = Paint.Align.LEFT

            y += 46f
            paint.color = SLATE
            paint.strokeWidth = 2f
            canvas.drawLine(96f, y, WIDTH - 96f, y, paint)
            y += step - 46f
        }

        return bitmap
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
