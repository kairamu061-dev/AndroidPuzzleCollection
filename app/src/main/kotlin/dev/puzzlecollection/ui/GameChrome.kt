package dev.puzzlecollection.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dev.puzzlecollection.R

@Composable
fun BackIconButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = stringResource(R.string.back),
        )
    }
}

@Composable
fun LoadingScreen() {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.testTag("progress"))
        }
    }
}

@Composable
fun ConfirmNewGameDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.confirm_new_game_title)) },
        text = { Text(stringResource(R.string.confirm_new_game_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.confirm_new_game_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** Not dismissible by tapping outside: the board behind it no longer accepts input. */
@Composable
fun ClearedDialog(message: String, onNewGame: () -> Unit, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.cleared_title)) },
        text = { Text(message) },
        confirmButton = { ClearedDialogButtons(onNewGame, onExit) },
    )
}

@Composable
fun ClearedDialogButtons(onNewGame: () -> Unit, onExit: () -> Unit) {
    Row {
        TextButton(onClick = onExit) { Text(stringResource(R.string.go_home)) }
        TextButton(onClick = onNewGame) { Text(stringResource(R.string.new_game)) }
    }
}
