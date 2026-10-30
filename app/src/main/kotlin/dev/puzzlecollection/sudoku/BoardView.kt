package dev.puzzlecollection.sudoku

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import dev.puzzlecollection.R
import dev.puzzlecollection.ui.BoardColors
import dev.puzzlecollection.ui.LocalBoardColors

@Composable
fun BoardView(game: GameState, onCellClick: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalBoardColors.current
    val conflicts = game.conflicts
    val selected = game.selected
    val selectedDigit = selected?.let { game.board.values[it] }?.takeIf { it != BLANK }

    BoxWithConstraints(modifier) {
        val cellSize = maxWidth / 9
        // Sizes derive from the cell, not from the user's font scale, or digits would clip.
        val digitSize = sizeFor(cellSize * 0.62f)
        val memoSize = sizeFor(cellSize * 0.25f)
        Column(Modifier.fillMaxSize().background(colors.cell)) {
            for (row in 0 until 9) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (column in 0 until 9) {
                        val cell = row * 9 + column
                        val value = game.board.values[cell]
                        CellView(
                            game = game,
                            cell = cell,
                            background = backgroundOf(cell, selected, selectedDigit, value, colors),
                            isConflict = cell in conflicts,
                            digitSize = digitSize,
                            memoSize = memoSize,
                            onClick = { onCellClick(cell) },
                            modifier = Modifier.weight(1f).fillMaxSize(),
                        )
                    }
                }
            }
        }
        GridLines(colors, Modifier.matchParentSize())
    }
}

@Composable
private fun sizeFor(size: Dp): TextUnit = with(LocalDensity.current) { size.toSp() }

private fun backgroundOf(
    cell: Int,
    selected: Int?,
    selectedDigit: Int?,
    value: Int,
    colors: BoardColors,
): Color = when {
    cell == selected -> colors.selected
    selectedDigit != null && value == selectedDigit -> colors.sameDigit
    selected != null && cell in Geometry.peers[selected] -> colors.peer
    else -> colors.cell
}

@Composable
private fun CellView(
    game: GameState,
    cell: Int,
    background: Color,
    isConflict: Boolean,
    digitSize: TextUnit,
    memoSize: TextUnit,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val colors = LocalBoardColors.current
    val value = game.board.values[cell]
    val isGiven = game.isGiven(cell)
    val description = cellDescription(game, cell, isConflict)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(background)
            .testTag("cell-$cell")
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        if (value != BLANK) {
            Text(
                text = value.toString(),
                style = cellTextStyle(digitSize),
                fontWeight = if (isGiven) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isConflict -> colors.conflict
                    isGiven -> colors.given
                    else -> colors.entered
                },
                modifier = Modifier.clearAndSetSemantics {},
            )
        } else if (game.board.memos[cell] != 0) {
            MemoGrid(game, cell, memoSize, colors.memo)
        }
    }
}

@Composable
private fun MemoGrid(game: GameState, cell: Int, size: TextUnit, color: Color) {
    Column(Modifier.fillMaxSize().clearAndSetSemantics {}) {
        DIGITS.chunked(3).forEach { row ->
            Row(Modifier.weight(1f).fillMaxWidth()) {
                row.forEach { digit ->
                    Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (game.hasMemo(cell, digit)) {
                            Text(digit.toString(), style = cellTextStyle(size), color = color)
                        }
                    }
                }
            }
        }
    }
}

private fun cellTextStyle(size: TextUnit) = TextStyle(
    fontSize = size,
    lineHeight = size,
    textAlign = TextAlign.Center,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
)

@Composable
private fun cellDescription(game: GameState, cell: Int, isConflict: Boolean): String {
    val value = game.board.values[cell]
    val content = when {
        value == BLANK -> stringResource(R.string.cell_empty)
        game.isGiven(cell) -> stringResource(R.string.cell_given, value)
        else -> stringResource(R.string.cell_entered, value)
    }
    val memos = DIGITS.filter { game.hasMemo(cell, it) }
    return buildString {
        append(stringResource(R.string.cell_position, cell / 9 + 1, cell % 9 + 1))
        append(' ').append(content)
        if (isConflict) append(' ').append(stringResource(R.string.cell_conflict))
        if (memos.isNotEmpty()) {
            append(' ').append(stringResource(R.string.cell_memo, memos.joinToString(" ")))
        }
    }
}

@Composable
private fun GridLines(colors: BoardColors, modifier: Modifier) {
    Canvas(modifier) {
        val step = size.width / 9
        val thin = 1.dp.toPx()
        val thick = 2.dp.toPx()
        for (pass in 0..1) {
            for (i in 0..9) {
                val isThick = i % 3 == 0
                if (isThick != (pass == 1)) continue
                val offset = (step * i).coerceIn(thick / 2, size.width - thick / 2)
                val color = if (isThick) colors.thickLine else colors.thinLine
                val width = if (isThick) thick else thin
                drawLine(color, Offset(offset, 0f), Offset(offset, size.height), width)
                drawLine(color, Offset(0f, offset), Offset(size.width, offset), width)
            }
        }
    }
}
