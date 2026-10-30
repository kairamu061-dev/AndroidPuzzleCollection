package dev.puzzlecollection.sudoku

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.puzzlecollection.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SudokuStartScreen(
    saved: SavedSummary?,
    onResume: () -> Unit,
    onNewGame: (Difficulty) -> Unit,
    onBack: () -> Unit,
) {
    var pendingDifficulty by rememberSaveable { mutableStateOf<Difficulty?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sudoku_title)) },
                navigationIcon = { BackIconButton(onBack) },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (saved != null) {
                Button(onClick = onResume, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(
                            R.string.resume_game,
                            difficultyLabel(saved.difficulty),
                            formatElapsed(saved.elapsedMillis),
                        ),
                    )
                }
            }
            Text(
                text = stringResource(R.string.new_game),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            Difficulty.entries.forEach { difficulty ->
                OutlinedButton(
                    onClick = {
                        if (saved == null) onNewGame(difficulty) else pendingDifficulty = difficulty
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(difficultyLabel(difficulty))
                }
            }
        }
    }

    pendingDifficulty?.let { difficulty ->
        AlertDialog(
            onDismissRequest = { pendingDifficulty = null },
            title = { Text(stringResource(R.string.confirm_new_game_title)) },
            text = { Text(stringResource(R.string.confirm_new_game_message)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDifficulty = null
                    onNewGame(difficulty)
                }) { Text(stringResource(R.string.confirm_new_game_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDifficulty = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
