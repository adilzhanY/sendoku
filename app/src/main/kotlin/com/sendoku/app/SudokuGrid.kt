package com.sendoku.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sendoku.engine.Board
import com.sendoku.engine.Digits

/**
 * A read-only grid. Cell selection, pencil marks, and input arrive with the game
 * screen; this draws what the generator produced so the engine is visible on a phone.
 */
@Composable
fun SudokuGrid(
    board: Board,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.outline
    val boxLineColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surface)
            .drawBehind {
                val cell = size.width / board.size
                for (line in 0..board.size) {
                    val onBoxEdge = line % board.dims.boxWidth == 0
                    val width = if (onBoxEdge) 3f else 1f
                    val color = if (onBoxEdge) boxLineColor else lineColor
                    val at = line * cell
                    drawLine(color, Offset(at, 0f), Offset(at, size.height), width)
                }
                for (line in 0..board.size) {
                    val onBoxEdge = line % board.dims.boxHeight == 0
                    val width = if (onBoxEdge) 3f else 1f
                    val color = if (onBoxEdge) boxLineColor else lineColor
                    val at = line * cell
                    drawLine(color, Offset(0f, at), Offset(size.width, at), width)
                }
            },
    ) {
        for (row in 0 until board.size) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                for (col in 0 until board.size) {
                    val digit = board[row, col]
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (digit != Board.EMPTY) {
                            Text(
                                text = Digits.toChar(digit).toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Unspecified,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewPuzzleButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier.padding(top = 8.dp)) {
        Text("New puzzle")
    }
}
