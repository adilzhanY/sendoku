package com.sendoku.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sendoku.engine.Dimensions
import com.sendoku.engine.Generator
import com.sendoku.engine.Puzzle
import com.sendoku.engine.Symmetry

/**
 * A placeholder screen. It exists to prove the engine module is wired into the app
 * and to give the grid somewhere to live. The real game screen comes with the
 * playable board, so nothing here is meant to survive.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { insets ->
                    var puzzle by remember {
                        mutableStateOf(Generator(Dimensions.CLASSIC).generate(Symmetry.ROTATIONAL))
                    }
                    ScaffoldContent(
                        puzzle = puzzle,
                        onNewPuzzle = { puzzle = Generator(Dimensions.CLASSIC).generate(Symmetry.ROTATIONAL) },
                        modifier = Modifier.padding(insets),
                    )
                }
            }
        }
    }
}

@Composable
private fun ScaffoldContent(
    puzzle: Puzzle,
    onNewPuzzle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        Text("Sendoku", style = MaterialTheme.typography.headlineLarge)
        SudokuGrid(board = puzzle.givens)
        Text(
            "${puzzle.clueCount} clues",
            style = MaterialTheme.typography.bodyMedium,
        )
        NewPuzzleButton(onClick = onNewPuzzle)
    }
}
