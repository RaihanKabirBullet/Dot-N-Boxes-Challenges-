package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.game.DotsAndBoxesEngine
import com.example.model.GameMode
import com.example.model.LineId
import com.example.model.LineType
import com.example.ui.GameViewModel
import com.example.ui.screens.OnBoardSetupScreen
import com.example.ui.screens.PlayMenuScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.DotsAndBoxesTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Dot N Box Challenge", appName)
    }

    @Test
    fun `splash screen displays title and creator credit`() {
        composeRule.setContent {
            DotsAndBoxesTheme {
                SplashScreen(onFinished = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("DOT N BOX CHALLENGE").assertIsDisplayed()
        composeRule.onNodeWithText("Created By Raihan Kabir").assertIsDisplayed()
    }

    @Test
    fun `play menu displays both options and triggers callbacks`() {
        var aiClicked = false
        var onBoardClicked = false
        composeRule.setContent {
            DotsAndBoxesTheme {
                PlayMenuScreen(
                    onWithAi = { aiClicked = true },
                    onOnBoardMultiplayer = { onBoardClicked = true },
                    onBack = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("WITH AI").assertIsDisplayed()
        composeRule.onNodeWithText("ON BOARD MULTIPLAYER").assertIsDisplayed()
        composeRule.onNodeWithTag("play_menu_with_ai").performClick()
        composeRule.onNodeWithTag("play_menu_on_board").performClick()
        assertTrue(aiClicked)
        assertTrue(onBoardClicked)
    }

    @Test
    fun `on board setup defaults to 2 players and starts match on button tap`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = GameViewModel(app)
        assertEquals(2, vm.onBoardPlayerCount.value)

        var started = false
        composeRule.setContent {
            DotsAndBoxesTheme {
                OnBoardSetupScreen(
                    playerCount = vm.onBoardPlayerCount.value,
                    selectedBoardSize = vm.onBoardBoardSize.value,
                    playerNames = vm.onBoardPlayerNames.value,
                    defaultNameProvider = vm::defaultOnBoardPlayerName,
                    onPlayerCountChange = vm::setOnBoardPlayerCount,
                    onBoardSizeChange = vm::setOnBoardBoardSize,
                    onPlayerNameChange = vm::setOnBoardPlayerName,
                    onStartGame = { started = true },
                    onBack = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("on_board_player_count_value").assertIsDisplayed()
        composeRule.onNodeWithTag("on_board_start_game_button").performClick()
        assertTrue(started)
    }

    @Test
    fun `viewmodel on board setup supports 2 to 10 players with default and custom names`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = GameViewModel(app)
        val expectedDefaults = listOf(
            "A Player", "B Player", "C Player", "D Player", "E Player",
            "F Player", "G Player", "H Player", "I Player", "J Player",
        )

        // Default count is 2
        assertEquals(2, vm.onBoardPlayerCount.value)

        // Customize player 2 and player 7 independently while leaving others blank
        vm.setOnBoardPlayerName(1, "Raihan")
        vm.setOnBoardPlayerName(6, "Kabir")
        vm.setOnBoardPlayerName(2, "   ") // whitespace must fall back to default

        for (count in 2..10) {
            vm.setOnBoardPlayerCount(count)
            assertEquals(count, vm.onBoardPlayerCount.value)
            vm.startOnBoardMatch()

            assertEquals(GameMode.ON_BOARD_LOCAL, vm.activeMode.value)
            val state = vm.onBoardGameState.value
            assertNotNull(state)
            assertEquals(count, state!!.players.size)

            for (idx in 0 until count) {
                val expectedName = when (idx) {
                    1 -> "Raihan"
                    6 -> "Kabir"
                    else -> expectedDefaults[idx]
                }
                assertEquals(expectedName, state.players[idx].name)
                assertEquals(idx, state.players[idx].colorIndex)
            }

            // First player draws a non-scoring line; turn advances to player 2 (index 1)
            vm.submitLineMove(LineId(LineType.HORIZONTAL, 0, 0))
            val afterMove = vm.onBoardGameState.value!!
            assertEquals(1, afterMove.currentTurnIndex)
            assertEquals(1, DotsAndBoxesEngine.availableLines(state).size - DotsAndBoxesEngine.availableLines(afterMove).size)
        }
    }
}
