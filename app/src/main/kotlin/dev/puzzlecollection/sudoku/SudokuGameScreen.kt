package dev.puzzlecollection.sudoku

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.puzzlecollection.R
import dev.puzzlecollection.session.TimedUiState
import dev.puzzlecollection.ui.BackIconButton
import dev.puzzlecollection.ui.ClearedDialog
import dev.puzzlecollection.ui.formatElapsed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SudokuGameScreen(
    state: TimedUiState.Playing<GameState>,
    viewModel: SudokuViewModel,
    onExit: () -> Unit,
) {
    val game = state.game
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(difficultyLabel(game.puzzle.difficulty)) },
                navigationIcon = { BackIconButton(viewModel::leaveGame) },
                actions = {
                    Text(
                        text = formatElapsed(state.elapsedMillis),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 16.dp).testTag("timer"),
                    )
                },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding).padding(8.dp)) {
            val board: @Composable (Modifier) -> Unit = { modifier ->
                Box(modifier, contentAlignment = Alignment.Center) {
                    BoardView(game, viewModel::selectCell, Modifier.aspectRatio(1f))
                }
            }
            val controls: @Composable (Modifier) -> Unit = { modifier ->
                Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionRow(game, viewModel)
                    NumberPad(game, viewModel::inputDigit)
                }
            }
            if (maxWidth > maxHeight) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    board(Modifier.weight(1f).fillMaxSize())
                    controls(Modifier.weight(1f).align(Alignment.CenterVertically))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // The board yields height so the controls stay on screen on short devices.
                    board(Modifier.weight(1f).fillMaxWidth())
                    controls(Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (game.isSolved) {
        ClearedDialog(
            message = stringResource(
                R.string.cleared_message,
                difficultyLabel(game.puzzle.difficulty),
                formatElapsed(state.elapsedMillis),
            ),
            onNewGame = viewModel::leaveGame,
            onExit = onExit,
        )
    }
}

@Composable
private fun ActionRow(game: GameState, viewModel: SudokuViewModel) {
    val compactPadding = PaddingValues(horizontal = 4.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = viewModel::undo,
            enabled = game.canUndo,
            contentPadding = compactPadding,
            modifier = Modifier.weight(1f),
        ) { Text(stringResource(R.string.action_undo)) }
        OutlinedButton(
            onClick = viewModel::redo,
            enabled = game.canRedo,
            contentPadding = compactPadding,
            modifier = Modifier.weight(1f),
        ) { Text(stringResource(R.string.action_redo)) }
        OutlinedButton(
            onClick = viewModel::erase,
            contentPadding = compactPadding,
            modifier = Modifier.weight(1f),
        ) { Text(stringResource(R.string.action_erase)) }
        val memoLabel = stringResource(
            if (game.memoMode) R.string.action_memo_on else R.string.action_memo_off,
        )
        if (game.memoMode) {
            Button(
                onClick = viewModel::toggleMemoMode,
                contentPadding = compactPadding,
                modifier = Modifier.weight(1f).testTag("memo"),
            ) { Text(memoLabel) }
        } else {
            OutlinedButton(
                onClick = viewModel::toggleMemoMode,
                contentPadding = compactPadding,
                modifier = Modifier.weight(1f).testTag("memo"),
            ) { Text(memoLabel) }
        }
    }
}

@Composable
private fun NumberPad(game: GameState, onDigit: (Int) -> Unit) {
    val remaining = game.remaining
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DIGITS.chunked(5).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { digit ->
                    val left = remaining[digit - 1]
                    Button(
                        onClick = { onDigit(digit) },
                        enabled = left > 0,
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1f).height(56.dp).testTag("digit-$digit"),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(digit.toString(), style = MaterialTheme.typography.titleLarge)
                            Text(left.toString(), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                if (row.size < 5) Spacer(Modifier.weight((5 - row.size).toFloat()))
            }
        }
    }
}
