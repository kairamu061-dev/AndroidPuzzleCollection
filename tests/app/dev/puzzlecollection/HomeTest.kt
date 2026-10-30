package dev.puzzlecollection

import android.content.pm.PackageManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun pressBack() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    @Test fun `I-01 home lists sudoku`() {
        rule.onNodeWithText("数独").assertIsDisplayed()
    }

    @Test fun `I-02 opening a game shows its start screen`() {
        rule.onNodeWithText("数独").performClick()
        rule.waitUntil { rule.onAllNodesWithTextExists("やさしい") }
        rule.onNodeWithText("やさしい").assertIsDisplayed()
    }

    @Test fun `I-03 back returns home and keeps the activity`() {
        rule.onNodeWithText("数独").performClick()
        rule.waitUntil { rule.onAllNodesWithTextExists("やさしい") }
        pressBack()
        rule.onNodeWithText("パズルコレクション").assertIsDisplayed()
        assertFalse(rule.activity.isFinishing)
    }

    /** The offline guarantee: nothing, including a dependency, may bring this permission in. */
    @Test fun `the merged manifest requests no network access`() {
        val packageInfo = rule.activity.packageManager.getPackageInfo(
            rule.activity.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        val permissions = packageInfo.requestedPermissions.orEmpty().toList()
        assertTrue(permissions.toString(), "android.permission.INTERNET" !in permissions)
    }
}
