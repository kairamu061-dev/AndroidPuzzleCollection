package dev.puzzlecollection.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

internal val LightColors = lightColorScheme(
    primary = Color(0xFF2F4B8F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE2F5),
    onPrimaryContainer = Color(0xFF14285A),
    secondary = Color(0xFF566080),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE2F5),
    onSecondaryContainer = Color(0xFF14285A),
    background = Color(0xFFF8F6F0),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFF8F6F0),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE9E6DD),
    surfaceContainerHighest = Color(0xFFE9E6DD),
    error = Color(0xFFBA1A1A),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFAFC3FF),
    onPrimary = Color(0xFF14285A),
    primaryContainer = Color(0xFF2F4B8F),
    onPrimaryContainer = Color(0xFFDCE2F5),
    secondary = Color(0xFFBDC6E4),
    onSecondary = Color(0xFF283048),
    secondaryContainer = Color(0xFF353C55),
    onSecondaryContainer = Color(0xFFDCE2F5),
    background = Color(0xFF14151A),
    onBackground = Color(0xFFE6E4E0),
    surface = Color(0xFF14151A),
    onSurface = Color(0xFFE6E4E0),
    surfaceVariant = Color(0xFF2A2C34),
    surfaceContainerHighest = Color(0xFF2A2C34),
    error = Color(0xFFFFB4AB),
)

@Immutable
data class BoardColors(
    val cell: Color,
    val peer: Color,
    val sameDigit: Color,
    val selected: Color,
    val given: Color,
    val entered: Color,
    val conflict: Color,
    val memo: Color,
    val thinLine: Color,
    val thickLine: Color,
)

internal val LightBoardColors = BoardColors(
    cell = Color(0xFFFFFFFF),
    peer = Color(0xFFE8EBF4),
    sameDigit = Color(0xFFCBD5EE),
    selected = Color(0xFFB4C4EA),
    given = Color(0xFF1C1B1F),
    entered = Color(0xFF2F4B8F),
    conflict = Color(0xFFBA1A1A),
    memo = Color(0xFF566080),
    thinLine = Color(0xFFC4C6D0),
    thickLine = Color(0xFF1C1B1F),
)

internal val DarkBoardColors = BoardColors(
    cell = Color(0xFF1D1F27),
    peer = Color(0xFF2A2E3B),
    sameDigit = Color(0xFF3A435C),
    selected = Color(0xFF4A5A85),
    given = Color(0xFFE6E4E0),
    entered = Color(0xFFAFC3FF),
    conflict = Color(0xFFFFB4AB),
    memo = Color(0xFFBDC6E4),
    thinLine = Color(0xFF44464F),
    thickLine = Color(0xFFE6E4E0),
)
