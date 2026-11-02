package dev.puzzlecollection.numberlink

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
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
import dev.puzzlecollection.ui.LocalBoardColors
import dev.puzzlecollection.ui.formatElapsed

const val NUMBERLINK_SAVE_PATH = "numberlink/save.txt"

@Composable
fun NumberlinkRoute(onExit: () -> Unit, viewModel: NumberlinkViewModel = numberlinkViewModel()) {
    TimedGameRoute(
        viewModel = viewModel,
        title = stringResource(R.string.numberlink_title),
        options = NumberlinkGenerator.SIZES.map { it to stringResource(R.string.size_label, it, it) },
        resumeLabel = { start ->
            start.saved?.let { saved ->
                stringResource(
                    R.string.resume_game,
                    stringResource(R.string.size_label, saved.puzzle.size, saved.puzzle.size),
                    formatElapsed(start.elapsedMillis),
                )
            }
        },
        onExit = onExit,
    ) { playing ->
        NumberlinkGameScreen(playing, viewModel) {
            viewModel.leaveGame()
            onExit()
        }
    }
}

@Composable
private fun numberlinkViewModel(): NumberlinkViewModel {
    val appContext = LocalContext.current.applicationContext
    return viewModel(
        factory = viewModelFactory {
            initializer { NumberlinkViewModel(SaveFile(appContext.filesDir, NUMBERLINK_SAVE_PATH)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NumberlinkGameScreen(
    state: TimedUiState.Playing<NumberlinkState>,
    viewModel: NumberlinkViewModel,
    onExit: () -> Unit,
) {
    val game = state.game
    val size = game.puzzle.size
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.size_label, size, size)) },
                navigationIcon = { BackIconButton(viewModel::leaveGame) },
                actions = {
                    Text(
                        text = formatElapsed(state.elapsedMillis),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            val board = @Composable { modifier: Modifier ->
                Box(modifier, contentAlignment = Alignment.Center) {
                    NumberlinkBoard(game, viewModel, Modifier.aspectRatio(1f))
                }
            }
            val controls = @Composable { modifier: Modifier ->
                Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatusText(game)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = viewModel::undo, enabled = game.canUndo, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.action_undo))
                        }
                        OutlinedButton(onClick = viewModel::redo, enabled = game.canRedo, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.action_redo))
                        }
                    }
                }
            }
            if (maxWidth > maxHeight) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    board(Modifier.weight(1f).fillMaxSize())
                    controls(Modifier.weight(1f).align(Alignment.CenterVertically))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                stringResource(R.string.size_label, size, size),
                formatElapsed(state.elapsedMillis),
            ),
            onNewGame = viewModel::leaveGame,
            onExit = onExit,
        )
    }
}

/** With the fill-all rule a player can join every pair and still not be done: say why. */
@Composable
private fun StatusText(game: NumberlinkState) {
    val joined = game.paths.indices.count(game::isConnected)
    val text = if (game.allConnected && !game.isSolved) {
        stringResource(R.string.numberlink_empty_cells, game.emptyCells)
    } else {
        stringResource(R.string.numberlink_connected, joined, game.paths.size)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = if (game.allConnected && !game.isSolved) MaterialTheme.colorScheme.error else Color.Unspecified,
        modifier = Modifier.fillMaxWidth().testTag("status"),
    )
}

@Composable
private fun NumberlinkBoard(game: NumberlinkState, viewModel: NumberlinkViewModel, modifier: Modifier) {
    val puzzle = game.puzzle
    val board = LocalBoardColors.current
    val dark = isSystemInDarkTheme()
    val colors = remember(puzzle.lines.size, dark) { List(puzzle.lines.size) { lineColor(it, dark) } }
    val measurer = rememberTextMeasurer()
    var pixelSize by remember { mutableStateOf(IntSize.Zero) }
    val layout = remember(puzzle.size, pixelSize) {
        NumberlinkLayout(puzzle.size, pixelSize.width.toFloat().coerceAtLeast(1f), pixelSize.height.toFloat().coerceAtLeast(1f))
    }
    val density = LocalDensity.current
    val labels = remember(layout, colors) {
        val fontSize = with(density) { (layout.cell * 0.42f).toSp() }
        colors.mapIndexed { line, color ->
            val ink = if (color.luminance() > 0.45f) Color.Black else Color.White
            measurer.measure((line + 1).toString(), TextStyle(fontSize = fontSize, color = ink, fontWeight = FontWeight.Bold))
        }
    }
    val currentLayout by rememberUpdatedState(layout)
    val description = stringResource(
        R.string.numberlink_board_description,
        game.paths.indices.count(game::isConnected),
        game.paths.size,
        game.emptyCells,
    )

    Canvas(
        modifier
            .onSizeChanged { pixelSize = it }
            .testTag("numberlink-board")
            .semantics { contentDescription = description }
            .pointerInput(viewModel) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val start = currentLayout.cellAt(down.position.x, down.position.y) ?: return@awaitEachGesture
                    down.consume()
                    viewModel.strokeStart(start)
                    var last = down.position
                    drag(down.id) { change ->
                        change.consume()
                        val at = change.position
                        currentLayout.cellsAlong(last.x, last.y, at.x, at.y).forEach(viewModel::strokeTo)
                        last = at
                    }
                    viewModel.strokeEnd()
                }
            },
    ) {
        val cell = layout.cell
        val sizePx = cell * puzzle.size
        drawRect(board.cell, Offset(layout.left, layout.top), Size(sizePx, sizePx))
        game.paths.forEachIndexed { line, path ->
            path.forEach { at ->
                drawRect(
                    colors[line].copy(alpha = 0.22f),
                    Offset(layout.left + at % puzzle.size * cell, layout.top + at / puzzle.size * cell),
                    Size(cell, cell),
                )
            }
        }
        for (i in 0..puzzle.size) {
            val at = i * cell
            val width = if (i == 0 || i == puzzle.size) 2.dp.toPx() else 1.dp.toPx()
            val color = if (i == 0 || i == puzzle.size) board.thickLine else board.thinLine
            drawLine(color, Offset(layout.left + at, layout.top), Offset(layout.left + at, layout.top + sizePx), width)
            drawLine(color, Offset(layout.left, layout.top + at), Offset(layout.left + sizePx, layout.top + at), width)
        }
        game.paths.forEachIndexed { line, path ->
            if (path.size < 2) return@forEachIndexed
            val stroke = Path().apply {
                moveTo(layout.centerX(path[0]), layout.centerY(path[0]))
                path.drop(1).forEach { lineTo(layout.centerX(it), layout.centerY(it)) }
            }
            drawPath(stroke, colors[line], style = Stroke(cell * 0.3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        puzzle.endpoints.forEachIndexed { line, (a, b) ->
            for (end in listOf(a, b)) {
                val center = Offset(layout.centerX(end), layout.centerY(end))
                drawCircle(colors[line], cell * 0.38f, center)
                val label = labels[line]
                drawText(label, topLeft = center - Offset(label.size.width / 2f, label.size.height / 2f))
            }
        }
    }
}

/** Hues spread by the golden angle so neighbouring numbers never get neighbouring colours. */
private fun lineColor(index: Int, dark: Boolean): Color {
    val hue = (index * 137.508f) % 360f
    val saturation = if (index % 2 == 0) 0.65f else 0.85f
    val value = if (dark) 0.95f else if (index % 3 == 0) 0.7f else 0.85f
    return Color.hsv(hue, saturation, value)
}
