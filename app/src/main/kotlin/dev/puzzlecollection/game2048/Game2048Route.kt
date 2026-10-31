package dev.puzzlecollection.game2048

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.puzzlecollection.R
import dev.puzzlecollection.data.SaveFile
import dev.puzzlecollection.ui.BackIconButton
import dev.puzzlecollection.ui.ClearedDialogButtons
import dev.puzzlecollection.ui.ConfirmNewGameDialog
import dev.puzzlecollection.ui.LoadingScreen
import kotlin.math.log2

const val GAME2048_SAVE_PATH = "game2048/save.txt"

private val SWIPE_THRESHOLD = 24.dp

@Composable
fun Game2048Route(onExit: () -> Unit, viewModel: Game2048ViewModel = game2048ViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onExit)
    val game = state.game
    if (game == null) {
        LoadingScreen()
    } else {
        Game2048Screen(state, game, viewModel, onExit)
    }
}

@Composable
private fun game2048ViewModel(): Game2048ViewModel {
    val appContext = LocalContext.current.applicationContext
    return viewModel(
        factory = viewModelFactory {
            initializer { Game2048ViewModel(SaveFile(appContext.filesDir, GAME2048_SAVE_PATH)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Game2048Screen(
    state: Game2048UiState,
    game: Game2048State,
    viewModel: Game2048ViewModel,
    onExit: () -> Unit,
) {
    var confirmingNewGame by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.game2048_title)) },
                navigationIcon = { BackIconButton(onExit) },
                actions = {
                    TextButton(onClick = {
                        if (game.isOver) viewModel.startNewGame() else confirmingNewGame = true
                    }) { Text(stringResource(R.string.new_game)) }
                },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.score_label, game.score),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    stringResource(R.string.best_label, game.best),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                TileBoard(game, viewModel::move, Modifier.aspectRatio(1f))
            }
        }
    }

    when {
        confirmingNewGame -> ConfirmNewGameDialog(
            onConfirm = {
                confirmingNewGame = false
                viewModel.startNewGame()
            },
            onDismiss = { confirmingNewGame = false },
        )
        state.showReached -> AlertDialog(
            onDismissRequest = viewModel::dismissReached,
            title = { Text(stringResource(R.string.game2048_reached_title)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissReached) {
                    Text(stringResource(R.string.game2048_continue))
                }
            },
        )
        game.isOver -> AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.game2048_over_title)) },
            text = { Text(stringResource(R.string.score_label, game.score)) },
            confirmButton = {
                ClearedDialogButtons(onNewGame = viewModel::startNewGame, onExit = onExit)
            },
        )
    }
}

@Composable
private fun TileBoard(game: Game2048State, onMove: (Direction) -> Unit, modifier: Modifier) {
    val threshold = with(LocalDensity.current) { SWIPE_THRESHOLD.toPx() }
    val currentOnMove by rememberUpdatedState(onMove)
    val colors = MaterialTheme.colorScheme
    var total by remember { mutableStateOf(Offset.Zero) }
    BoxWithConstraints(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceVariant)
            .testTag("board")
            .pointerInput(threshold) {
                detectDragGestures(
                    onDragStart = { total = Offset.Zero },
                    onDragEnd = { dragDirection(total.x, total.y, threshold)?.let { currentOnMove(it) } },
                    onDrag = { change, amount ->
                        change.consume()
                        total += amount
                    },
                )
            },
    ) {
        val cell = maxWidth / BOARD_SIZE
        for (row in 0 until BOARD_SIZE) for (column in 0 until BOARD_SIZE) {
            Box(
                Modifier
                    .offset(cell * column, cell * row)
                    .size(cell)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.background),
            )
        }
        game.tiles.forEach { tile ->
            key(tile.id) { TileView(tile, cell) }
        }
    }
}

@Composable
private fun TileView(tile: Tile, cell: Dp) {
    val x by animateDpAsState(cell * tile.column, label = "tileX")
    val y by animateDpAsState(cell * tile.row, label = "tileY")
    val background = tileColor(tile.value)
    val digits = tile.value.toString().length
    val fontSize = with(LocalDensity.current) { (cell * (0.42f - 0.05f * (digits - 1))).toSp() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .offset(x, y)
            .size(cell)
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background),
    ) {
        Text(
            text = tile.value.toString(),
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = if (background.luminance() > 0.35f) Color(0xFF14285A) else Color.White,
        )
    }
}

/** Paper to indigo by doubling, reaching full strength at [WIN_VALUE]. */
@Composable
private fun tileColor(value: Int): Color {
    val strength = (log2(value.toFloat()) - 1f) / (log2(WIN_VALUE.toFloat()) - 1f)
    return lerp(Color(0xFFDCE2F5), Color(0xFF2F4B8F), strength.coerceIn(0f, 1f))
}
