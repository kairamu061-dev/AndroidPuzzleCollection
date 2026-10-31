package dev.puzzlecollection.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

/** [resumeLabel] null means no saved game, so starting one needs no confirmation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameStartScreen(
    title: String,
    resumeLabel: String?,
    optionLabels: List<String>,
    onResume: () -> Unit,
    onOption: (Int) -> Unit,
    onBack: () -> Unit,
) {
    var pendingOption by rememberSaveable { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(title) }, navigationIcon = { BackIconButton(onBack) })
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
            if (resumeLabel != null) {
                Button(onClick = onResume, modifier = Modifier.fillMaxWidth()) {
                    Text(resumeLabel)
                }
            }
            Text(
                text = stringResource(R.string.new_game),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            optionLabels.forEachIndexed { index, label ->
                OutlinedButton(
                    onClick = { if (resumeLabel == null) onOption(index) else pendingOption = index },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(label)
                }
            }
        }
    }

    pendingOption?.let { option ->
        ConfirmNewGameDialog(
            onConfirm = {
                pendingOption = null
                onOption(option)
            },
            onDismiss = { pendingOption = null },
        )
    }
}
