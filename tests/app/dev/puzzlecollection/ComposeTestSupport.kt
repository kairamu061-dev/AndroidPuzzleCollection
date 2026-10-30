package dev.puzzlecollection

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText

fun ComposeTestRule.onAllNodesWithTextExists(text: String): Boolean =
    onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
