package dev.puzzlecollection.nonogram

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.puzzlecollection.R
import dev.puzzlecollection.data.SaveFile
import dev.puzzlecollection.session.TimedGameRoute
import dev.puzzlecollection.session.TimedUiState
import dev.puzzlecollection.ui.BackIconButton
import dev.puzzlecollection.ui.BoardColors
import dev.puzzlecollection.ui.ClearedDialog
import dev.puzzlecollection.ui.LocalBoardColors
import dev.puzzlecollection.ui.formatElapsed

const val NONOGRAM_SAVE_PATH = "nonogram/save.txt"

private const val BLOCK = 5

@Composable
fun NonogramRoute(onExit: () -> Unit, viewModel: NonogramViewModel = nonogramViewModel()) {
    TimedGameRoute(
        viewModel = viewModel,
        title = stringResource(R.string.nonogram_title),
        options = NONOGRAM_SIZES.map { it to stringResource(R.string.size_label, it, it) },
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
        NonogramGameScreen(playing, viewModel) {
            viewModel.leaveGame()
            onExit()
        }
    }
}

@Composable
private fun nonogramViewModel(): NonogramViewModel {
    val appContext = LocalContext.current.applicationContext
    return viewModel(
        factory = viewModelFactory {
            initializer { NonogramViewModel(SaveFile(appContext.filesDir, NONOGRAM_SAVE_PATH)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NonogramGameScreen(
    state: TimedUiState.Playing<NonogramState>,
    viewModel: NonogramViewModel,
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
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding).padding(8.dp)) {
            val board = @Composable { modifier: Modifier ->
                Box(modifier, contentAlignment = Alignment.Center) {
                    NonogramBoard(game, viewModel, Modifier.aspectRatio(1f))
                }
            }
            if (maxWidth > maxHeight) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    board(Modifier.weight(1f).fillMaxSize())
                    ToolRow(game, viewModel, Modifier.weight(1f).align(Alignment.CenterVertically))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    board(Modifier.weight(1f).fillMaxWidth())
                    ToolRow(game, viewModel, Modifier.fillMaxWidth())
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

@Composable
private fun ToolRow(game: NonogramState, viewModel: NonogramViewModel, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = viewModel::undo, enabled = game.canUndo, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.action_undo))
        }
        OutlinedButton(onClick = viewModel::redo, enabled = game.canRedo, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.action_redo))
        }
        ToolButton(game, viewModel, Mark.FILLED, R.string.tool_fill, Modifier.weight(1f))
        ToolButton(game, viewModel, Mark.CROSS, R.string.tool_cross, Modifier.weight(1f))
    }
}

@Composable
private fun ToolButton(
    game: NonogramState,
    viewModel: NonogramViewModel,
    tool: Mark,
    label: Int,
    modifier: Modifier,
) {
    if (game.tool == tool) {
        Button(onClick = {}, modifier = modifier) { Text(stringResource(label)) }
    } else {
        OutlinedButton(onClick = { viewModel.selectTool(tool) }, modifier = modifier) {
            Text(stringResource(label))
        }
    }
}

@Composable
private fun NonogramBoard(game: NonogramState, viewModel: NonogramViewModel, modifier: Modifier) {
    val puzzle = game.puzzle
    val colors = LocalBoardColors.current
    val dimmed = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val measurer = rememberTextMeasurer()
    var pixelSize by remember { mutableStateOf(IntSize.Zero) }
    val layout = remember(puzzle, pixelSize) {
        NonogramLayout(
            size = puzzle.size,
            maxRowClues = puzzle.rowClues.maxOf { it.size },
            maxColumnClues = puzzle.columnClues.maxOf { it.size },
            width = pixelSize.width.toFloat().coerceAtLeast(1f),
            height = pixelSize.height.toFloat().coerceAtLeast(1f),
        )
    }
    val currentLayout by rememberUpdatedState(layout)
    val description = stringResource(
        R.string.nonogram_board_description,
        game.cells.count { it == Mark.FILLED },
        game.cells.count { it == Mark.CROSS },
    )

    Canvas(
        modifier
            .onSizeChanged { pixelSize = it }
            .testTag("nonogram-board")
            .semantics { contentDescription = description }
            .pointerInput(viewModel) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val first = currentLayout.cellAt(down.position.x, down.position.y) ?: return@awaitEachGesture
                    down.consume()
                    viewModel.strokeStart(first)
                    drag(down.id) { change ->
                        change.consume()
                        currentLayout.cellAt(change.position.x, change.position.y)?.let(viewModel::strokeTo)
                    }
                    viewModel.strokeEnd()
                }
            },
    ) {
        drawGrid(game, layout, colors)
        val fontSize = (layout.clueStep / density * 0.82f / fontScale).sp
        val base = TextStyle(fontSize = fontSize, color = colors.given)
        fun clue(text: String, centerX: Float, centerY: Float, done: Boolean) {
            val measured = measurer.measure(text, if (done) base.copy(color = dimmed) else base)
            drawText(
                measured,
                topLeft = Offset(centerX - measured.size.width / 2f, centerY - measured.size.height / 2f),
            )
        }
        puzzle.rowClues.forEachIndexed { row, clues ->
            val y = layout.gridTop + (row + 0.5f) * layout.cell
            val done = game.isRowDone(row)
            clues.forEachIndexed { i, value ->
                val x = layout.gridLeft - (clues.size - i - 0.5f) * layout.clueStep
                clue(value.toString(), x, y, done)
            }
        }
        puzzle.columnClues.forEachIndexed { column, clues ->
            val x = layout.gridLeft + (column + 0.5f) * layout.cell
            val done = game.isColumnDone(column)
            clues.forEachIndexed { i, value ->
                val y = layout.gridTop - (clues.size - i - 0.5f) * layout.clueStep
                clue(value.toString(), x, y, done)
            }
        }
    }
}

private fun DrawScope.drawGrid(game: NonogramState, layout: NonogramLayout, colors: BoardColors) {
    val size = game.puzzle.size
    val cell = layout.cell
    val left = layout.gridLeft
    val top = layout.gridTop
    drawRect(colors.cell, Offset(left, top), Size(layout.gridSize, layout.gridSize))
    game.focus?.let { focus ->
        drawRect(colors.peer, Offset(left, top + focus / size * cell), Size(layout.gridSize, cell))
        drawRect(colors.peer, Offset(left + focus % size * cell, top), Size(cell, layout.gridSize))
    }
    game.cells.forEachIndexed { index, mark ->
        val x = left + index % size * cell
        val y = top + index / size * cell
        when (mark) {
            Mark.EMPTY -> Unit
            Mark.FILLED -> drawRect(colors.entered, Offset(x, y), Size(cell, cell))
            Mark.CROSS -> drawCross(x, y, cell, colors.memo)
        }
    }
    for (pass in 0..1) {
        for (i in 0..size) {
            val thick = i % BLOCK == 0 || i == size
            if (thick != (pass == 1)) continue
            val color = if (thick) colors.thickLine else colors.thinLine
            val width = (if (thick) 2.dp else 1.dp).toPx()
            val at = i * cell
            drawLine(color, Offset(left + at, top), Offset(left + at, top + layout.gridSize), width)
            drawLine(color, Offset(left, top + at), Offset(left + layout.gridSize, top + at), width)
        }
    }
}

private fun DrawScope.drawCross(x: Float, y: Float, cell: Float, color: Color) {
    val inset = cell * 0.28f
    val far = cell - inset
    val width = 1.5.dp.toPx()
    drawLine(color, Offset(x + inset, y + inset), Offset(x + far, y + far), width)
    drawLine(color, Offset(x + far, y + inset), Offset(x + inset, y + far), width)
}
