package dev.puzzlecollection.nonogram

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.ui.graphics.drawscope.clipRect
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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.puzzlecollection.R
import dev.puzzlecollection.data.SaveFile
import dev.puzzlecollection.session.TimedGameRoute
import dev.puzzlecollection.session.TimedUiState
import dev.puzzlecollection.ui.BackIconButton
import dev.puzzlecollection.ui.BoardColors
import dev.puzzlecollection.ui.ClearedDialogButtons
import dev.puzzlecollection.ui.LocalBoardColors
import dev.puzzlecollection.ui.formatElapsed

const val NONOGRAM_SAVE_PATH = "nonogram/save.txt"
const val NONOGRAM_PROGRESS_PATH = "nonogram/progress.txt"

private const val GALLERY_COLUMNS = 5

private const val BLOCK = 5

@Composable
fun NonogramRoute(onExit: () -> Unit, viewModel: NonogramViewModel = nonogramViewModel()) {
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val pictures = catalog.pictures
    val next = nextPicture(pictures, catalog.completed)
    TimedGameRoute(
        viewModel = viewModel,
        title = stringResource(R.string.nonogram_title),
        options = pictures.mapIndexed { index, picture ->
            picture to if (picture == next) stringResource(R.string.nonogram_next, index + 1) else null
        },
        resumeLabel = { start ->
            start.saved?.let { saved ->
                stringResource(
                    R.string.resume_game,
                    gameTitle(saved, pictures),
                    formatElapsed(start.elapsedMillis),
                )
            }
        },
        onExit = onExit,
        startExtra = { pick -> PictureGallery(catalog, pick) },
    ) { playing ->
        NonogramGameScreen(playing, viewModel, pictures) {
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
            initializer {
                NonogramViewModel(
                    SaveFile(appContext.filesDir, NONOGRAM_SAVE_PATH),
                    SaveFile(appContext.filesDir, NONOGRAM_PROGRESS_PATH),
                )
            }
        },
    )
}

/** Names the puzzle by number: showing the picture's name would give the answer away. */
@Composable
private fun gameTitle(game: NonogramState, pictures: List<NonogramPicture>): String {
    val index = pictures.indexOfFirst { it.id == game.pictureId }
    return if (index >= 0) {
        stringResource(R.string.nonogram_number, index + 1)
    } else {
        stringResource(R.string.size_label, game.puzzle.size, game.puzzle.size)
    }
}

@Composable
private fun PictureGallery(catalog: NonogramCatalog, onPick: (Int) -> Unit) {
    Text(
        text = stringResource(R.string.nonogram_completed, catalog.completed.count { id -> catalog.pictures.any { it.id == id } }, catalog.pictures.size),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 12.dp),
    )
    catalog.pictures.withIndex().chunked(GALLERY_COLUMNS).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { (index, picture) ->
                PictureThumbnail(picture, picture.id in catalog.completed, Modifier.weight(1f)) {
                    onPick(index)
                }
            }
            repeat(GALLERY_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun PictureThumbnail(
    picture: NonogramPicture,
    completed: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val colors = LocalBoardColors.current
    if (completed) {
        Canvas(
            modifier
                .aspectRatio(1f)
                .clickable(onClick = onClick)
                .semantics { contentDescription = picture.name },
        ) {
            drawRect(colors.cell)
            val size = picture.puzzle.size
            val cell = this.size.width / size
            picture.puzzle.solution.forEachIndexed { index, filled ->
                if (filled) {
                    drawRect(
                        colors.entered,
                        Offset(index % size * cell, index / size * cell),
                        Size(cell, cell),
                    )
                }
            }
        }
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Text(stringResource(R.string.nonogram_unknown))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NonogramGameScreen(
    state: TimedUiState.Playing<NonogramState>,
    viewModel: NonogramViewModel,
    pictures: List<NonogramPicture>,
    onExit: () -> Unit,
) {
    val game = state.game
    val size = game.puzzle.size
    var viewport by remember(size) { mutableStateOf(NonogramViewport()) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(gameTitle(game, pictures)) },
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
                    NonogramBoard(game, viewModel, viewport, { viewport = it }, Modifier.aspectRatio(1f))
                }
            }
            if (maxWidth > maxHeight) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    board(Modifier.weight(1f).fillMaxSize())
                    val controls = Modifier.weight(1f).align(Alignment.CenterVertically)
                    if (game.isSolved) {
                        ClearedBar(state, pictures, viewModel, onExit, controls)
                    } else {
                        ToolRow(game, viewModel, viewport, { viewport = NonogramViewport() }, controls)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    board(Modifier.weight(1f).fillMaxWidth())
                    val controls = Modifier.fillMaxWidth()
                    if (game.isSolved) {
                        ClearedBar(state, pictures, viewModel, onExit, controls)
                    } else {
                        ToolRow(game, viewModel, viewport, { viewport = NonogramViewport() }, controls)
                    }
                }
            }
        }
    }
}

/** Shown under the board instead of a dialog: the finished picture is the reward. */
@Composable
private fun ClearedBar(
    state: TimedUiState.Playing<NonogramState>,
    pictures: List<NonogramPicture>,
    viewModel: NonogramViewModel,
    onExit: () -> Unit,
    modifier: Modifier,
) {
    val game = state.game
    val name = pictures.firstOrNull { it.id == game.pictureId }?.name ?: gameTitle(game, pictures)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.cleared_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.cleared_message, name, formatElapsed(state.elapsedMillis)))
        ClearedDialogButtons(onNewGame = viewModel::leaveGame, onExit = onExit)
    }
}

@Composable
private fun ToolRow(
    game: NonogramState,
    viewModel: NonogramViewModel,
    viewport: NonogramViewport,
    onResetZoom: () -> Unit,
    modifier: Modifier,
) {
    val padding = PaddingValues(horizontal = 4.dp)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = viewModel::undo,
            enabled = game.canUndo,
            contentPadding = padding,
            modifier = Modifier.weight(1f),
        ) { Text(stringResource(R.string.action_undo)) }
        OutlinedButton(
            onClick = viewModel::redo,
            enabled = game.canRedo,
            contentPadding = padding,
            modifier = Modifier.weight(1f),
        ) { Text(stringResource(R.string.action_redo)) }
        OutlinedButton(
            onClick = onResetZoom,
            enabled = viewport.isZoomed,
            contentPadding = padding,
            modifier = Modifier.weight(1f),
        ) { Text(stringResource(R.string.action_show_all)) }
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
    val padding = PaddingValues(horizontal = 4.dp)
    if (game.tool == tool) {
        Button(onClick = {}, contentPadding = padding, modifier = modifier) { Text(stringResource(label)) }
    } else {
        OutlinedButton(
            onClick = { viewModel.selectTool(tool) },
            contentPadding = padding,
            modifier = modifier,
        ) { Text(stringResource(label)) }
    }
}

@Composable
private fun NonogramBoard(
    game: NonogramState,
    viewModel: NonogramViewModel,
    viewport: NonogramViewport,
    onViewportChange: (NonogramViewport) -> Unit,
    modifier: Modifier,
) {
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
    val density = LocalDensity.current
    // Measured once: pinching redraws every frame, past what TextMeasurer's small cache holds.
    val digits = remember(layout, puzzle) {
        val style = TextStyle(fontSize = with(density) { (layout.clueStep * 0.82f).toSp() })
        (0..puzzle.size).associateWith { measurer.measure(it.toString(), style) }
    }
    val currentLayout by rememberUpdatedState(layout)
    val currentViewport by rememberUpdatedState(viewport)
    val currentOnViewportChange by rememberUpdatedState(onViewportChange)
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
                    var stroking = false
                    currentLayout.cellAt(down.position.x, down.position.y, currentViewport)?.let {
                        viewModel.strokeStart(it)
                        stroking = true
                    }
                    down.consume()
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            if (stroking) viewModel.strokeCancel()
                            stroking = false
                            val centroid = event.calculateCentroid()
                            val pan = event.calculatePan()
                            currentOnViewportChange(
                                currentViewport
                                    .zoomedBy(event.calculateZoom(), centroid.x, centroid.y, currentLayout)
                                    .pannedBy(pan.x, pan.y, currentLayout),
                            )
                        } else if (stroking && pressed.size == 1) {
                            val at = pressed.single().position
                            currentLayout.cellAt(at.x, at.y, currentViewport)?.let(viewModel::strokeTo)
                        }
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                    if (stroking) viewModel.strokeEnd()
                }
            },
    ) {
        val zoomed = layout.cell * viewport.scale
        clipRect(layout.gridLeft, layout.gridTop, layout.gridLeft + layout.gridSize, layout.gridTop + layout.gridSize) {
            drawGrid(game, layout, viewport, colors)
        }
        fun clue(value: Int, centerX: Float, centerY: Float, done: Boolean) {
            val measured = digits.getValue(value)
            drawText(
                measured,
                color = if (done) dimmed else colors.given,
                topLeft = Offset(centerX - measured.size.width / 2f, centerY - measured.size.height / 2f),
            )
        }
        clipRect(0f, layout.gridTop, layout.gridLeft, layout.gridTop + layout.gridSize) {
            puzzle.rowClues.forEachIndexed { row, clues ->
                val y = layout.gridTop + (row + 0.5f) * zoomed - viewport.panY
                val done = game.isRowDone(row)
                clues.forEachIndexed { i, value ->
                    clue(value, layout.gridLeft - (clues.size - i - 0.5f) * layout.clueStep, y, done)
                }
            }
        }
        clipRect(layout.gridLeft, 0f, layout.gridLeft + layout.gridSize, layout.gridTop) {
            puzzle.columnClues.forEachIndexed { column, clues ->
                val x = layout.gridLeft + (column + 0.5f) * zoomed - viewport.panX
                val done = game.isColumnDone(column)
                clues.forEachIndexed { i, value ->
                    clue(value, x, layout.gridTop - (clues.size - i - 0.5f) * layout.clueStep, done)
                }
            }
        }
    }
}

private fun DrawScope.drawGrid(
    game: NonogramState,
    layout: NonogramLayout,
    viewport: NonogramViewport,
    colors: BoardColors,
) {
    val size = game.puzzle.size
    val cell = layout.cell * viewport.scale
    val left = layout.gridLeft - viewport.panX
    val top = layout.gridTop - viewport.panY
    val full = cell * size
    drawRect(colors.cell, Offset(left, top), Size(full, full))
    game.focus?.takeUnless { game.isSolved }?.let { focus ->
        drawRect(colors.peer, Offset(left, top + focus / size * cell), Size(full, cell))
        drawRect(colors.peer, Offset(left + focus % size * cell, top), Size(cell, full))
    }
    game.cells.forEachIndexed { index, mark ->
        val x = left + index % size * cell
        val y = top + index / size * cell
        when (mark) {
            Mark.EMPTY -> Unit
            Mark.FILLED -> drawRect(colors.entered, Offset(x, y), Size(cell, cell))
            Mark.CROSS -> if (!game.isSolved) drawCross(x, y, cell, colors.memo)
        }
    }
    for (pass in 0..1) {
        for (i in 0..size) {
            val thick = i % BLOCK == 0 || i == size
            if (thick != (pass == 1)) continue
            val color = if (thick) colors.thickLine else colors.thinLine
            val width = (if (thick) 2.dp else 1.dp).toPx()
            val at = i * cell
            drawLine(color, Offset(left + at, top), Offset(left + at, top + full), width)
            drawLine(color, Offset(left, top + at), Offset(left + full, top + at), width)
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
