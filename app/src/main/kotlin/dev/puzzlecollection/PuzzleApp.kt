package dev.puzzlecollection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
fun PuzzleApp() {
    var openGameId by rememberSaveable { mutableStateOf<String?>(null) }
    val openGame = GameCatalog.games.firstOrNull { it.id == openGameId }
    if (openGame == null) {
        HomeScreen(GameCatalog.games, onOpen = { openGameId = it.id })
    } else {
        openGame.content { openGameId = null }
    }
}
