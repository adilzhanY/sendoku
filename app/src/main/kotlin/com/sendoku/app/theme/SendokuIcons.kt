package com.sendoku.app.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The icon set, drawn rather than imported.
 *
 * Material's extended icon library is about six megabytes of glyphs to use five of, on an app
 * whose whole download is one and a half. These are stroked outlines on a twenty four unit
 * grid with round caps, which is what makes them read as soft rather than stamped, and they
 * tint themselves from whatever colour the theme hands them.
 *
 * Deliberately not traced from anybody else's set. The shapes are the obvious ones for what
 * they mean, and the family resemblance comes from one stroke weight and one corner radius
 * rather than from a source.
 */
public object SendokuIcons {

    private const val SIZE = 24f
    private const val STROKE = 1.9f

    private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = SIZE.dp,
        defaultHeight = SIZE.dp,
        viewportWidth = SIZE,
        viewportHeight = SIZE,
    ).apply(block).build()

    private fun ImageVector.Builder.stroke(pathData: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = pathData,
        )
    }

    /** A loop turning back on itself, with the arrow head where the motion ends. */
    public val Undo: ImageVector = icon("Undo") {
        stroke {
            moveTo(4.5f, 9.5f)
            horizontalLineTo(14.0f)
            arcTo(5.0f, 5.0f, 0f, isMoreThanHalf = false, isPositiveArc = true, 14.0f, 19.5f)
            horizontalLineTo(8.5f)
        }
        stroke {
            moveTo(8.5f, 5.0f)
            lineTo(4.0f, 9.5f)
            lineTo(8.5f, 14.0f)
        }
    }

    /** The same loop mirrored. Redo is undo run the other way and should look like it. */
    public val Redo: ImageVector = icon("Redo") {
        stroke {
            moveTo(19.5f, 9.5f)
            horizontalLineTo(10.0f)
            arcTo(5.0f, 5.0f, 0f, isMoreThanHalf = false, isPositiveArc = false, 10.0f, 19.5f)
            horizontalLineTo(15.5f)
        }
        stroke {
            moveTo(15.5f, 5.0f)
            lineTo(20.0f, 9.5f)
            lineTo(15.5f, 14.0f)
        }
    }

    /** A block eraser held at an angle, with the line it rubs along underneath. */
    public val Erase: ImageVector = icon("Erase") {
        stroke {
            moveTo(9.4f, 18.6f)
            lineTo(4.6f, 13.8f)
            arcTo(1.6f, 1.6f, 0f, isMoreThanHalf = false, isPositiveArc = true, 4.6f, 11.5f)
            lineTo(12.2f, 3.9f)
            arcTo(1.6f, 1.6f, 0f, isMoreThanHalf = false, isPositiveArc = true, 14.5f, 3.9f)
            lineTo(19.3f, 8.7f)
            arcTo(1.6f, 1.6f, 0f, isMoreThanHalf = false, isPositiveArc = true, 19.3f, 11.0f)
            lineTo(11.7f, 18.6f)
            close()
        }
        stroke {
            moveTo(8.3f, 7.8f)
            lineTo(15.4f, 14.9f)
        }
        stroke {
            moveTo(4.0f, 21.0f)
            horizontalLineTo(20.0f)
        }
    }

    /** A pencil, for the pencil marks. Nib at the bottom left, where a right hand puts it. */
    public val Notes: ImageVector = icon("Notes") {
        stroke {
            moveTo(15.2f, 3.9f)
            lineTo(20.1f, 8.8f)
        }
        stroke {
            moveTo(17.0f, 2.1f)
            arcTo(1.7f, 1.7f, 0f, isMoreThanHalf = false, isPositiveArc = true, 19.4f, 2.1f)
            lineTo(21.9f, 4.6f)
            arcTo(1.7f, 1.7f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21.9f, 7.0f)
            lineTo(8.4f, 20.5f)
            lineTo(2.6f, 21.4f)
            lineTo(3.5f, 15.6f)
            close()
        }
    }

    /** A bulb with the light coming off it. A hint is an idea, not an answer. */
    public val Hint: ImageVector = icon("Hint") {
        stroke {
            moveTo(9.2f, 17.5f)
            arcTo(6.2f, 6.2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 14.8f, 17.5f)
            verticalLineTo(19.2f)
            horizontalLineTo(9.2f)
            close()
        }
        stroke {
            moveTo(9.8f, 21.6f)
            horizontalLineTo(14.2f)
        }
    }

    /** Two bars. The clock is stopped, not rewound. */
    public val Pause: ImageVector = icon("Pause") {
        stroke {
            moveTo(9.5f, 6.5f)
            verticalLineTo(17.5f)
        }
        stroke {
            moveTo(14.5f, 6.5f)
            verticalLineTo(17.5f)
        }
    }

    /** The way back. A chevron, because an arrow with a tail reads as a share on some phones. */
    public val Back: ImageVector = icon("Back") {
        stroke {
            moveTo(15.0f, 4.5f)
            lineTo(7.5f, 12.0f)
            lineTo(15.0f, 19.5f)
        }
    }

    /**
     * Settings. Sliders, not a cog.
     *
     * A cog at twenty two density pixels with a round stroke turns into a sun, which is what
     * the first attempt looked like. Two sliders read as adjustment at any size.
     */
    public val Settings: ImageVector = icon("Settings") {
        stroke {
            moveTo(4.0f, 8.0f)
            horizontalLineTo(20.0f)
        }
        stroke {
            moveTo(4.0f, 16.0f)
            horizontalLineTo(20.0f)
        }
        stroke {
            moveTo(9.0f, 5.6f)
            verticalLineTo(10.4f)
        }
        stroke {
            moveTo(15.0f, 13.6f)
            verticalLineTo(18.4f)
        }
    }
}
