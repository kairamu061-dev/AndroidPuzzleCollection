package dev.puzzlecollection.slide

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.puzzlecollection.R
import dev.puzzlecollection.data.SaveFile
import dev.puzzlecollection.session.TimedGameRoute
import dev.puzzlecollection.session.TimedUiState
import dev.puzzlecollection.ui.BackIconButton
import dev.puzzlecollection.ui.ClearedDialog
import dev.puzzlecollection.ui.formatElapsed

const val SLIDE_SAVE_PATH = "slide/save.txt"

@Composable
fun SlideRoute(onExit: () -> Unit, viewModel: SlideViewModel = slideViewModel()) {
    TimedGameRoute(
        viewModel = viewModel,
        title = stringResource(R.string.slide_title),
        options = SLIDE_SIZES.map { it to stringResource(R.string.size_label, it, it) },
        resumeLabel = { start ->
            start.saved?.let { saved ->
                stringResource(
                    R.string.resume_game,
                    stringResource(R.string.size_label, saved.size, saved.size),
                    formatElapsed(start.elapsedMillis),
                )
            }
        },
        onExit = onExit,
    ) { playing ->
        SlideGameScreen(playing, viewModel) {
            viewModel.leaveGame()
            onExit()
        }
    }
}

@Composable
private fun slideViewModel(): SlideViewModel {
    val appContext = LocalContext.current.applicationContext
    return viewModel(
        factory = viewModelFactory {
            initializer { SlideViewModel(SaveFile(appContext.filesDir, SLIDE_SAVE_PATH)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlideGameScreen(
    state: TimedUiState.Playing<SlideState>,
    viewModel: SlideViewModel,
    onExit: () -> Unit,
) {
    val game = state.game
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.size_label, game.size, game.size)) },
                navigationIcon = { BackIconButton(viewModel::leaveGame) },
                actions = {
                    Text(
                        text = stringResource(R.string.moves_label, game.moves) + "  " +
                            formatElapsed(state.elapsedMillis),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 16.dp).testTag("status"),
                    )
                },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            SlideBoard(game, viewModel::tap, Modifier.aspectRatio(1f))
        }
    }
    if (game.isSolved) {
        ClearedDialog(
            message = stringResource(
                R.string.slide_cleared_message,
                game.moves,
                formatElapsed(state.elapsedMillis),
            ),
            onNewGame = viewModel::leaveGame,
            onExit = onExit,
        )
    }
}

@Composable
private fun SlideBoard(game: SlideState, onTap: (Int) -> Unit, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        val cell = maxWidth / game.size
        val fontSize = with(LocalDensity.current) { (cell * 0.42f).toSp() }
        game.tiles.forEachIndexed { index, tile ->
            if (tile == BLANK_TILE) return@forEachIndexed
            // Keyed by tile number so the same tile animates instead of being redrawn in place.
            key(tile) {
                val x by animateDpAsState(cell * (index % game.size), label = "tileX")
                val y by animateDpAsState(cell * (index / game.size), label = "tileY")
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset(x, y)
                        .size(cell)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .testTag("tile-$tile")
                        .clickable { onTap(index) },
                ) {
                    Text(
                        text = tile.toString(),
                        fontSize = fontSize,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}
