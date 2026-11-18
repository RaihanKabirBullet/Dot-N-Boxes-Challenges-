package com.example

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.UserSettings
import com.example.game.DotsAndBoxesEngine
import com.example.game.MoveResult
import com.example.model.BoardSize
import com.example.model.GameMode
import com.example.model.LineId
import com.example.model.LineType
import com.example.model.PlayerInfo
import com.example.network.ClientSessionSnapshot
import com.example.network.DiscoveredRoom
import com.example.ui.GameViewModel
import com.example.ui.components.DotsAndBoxesBoard
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.JoinRoomScreen
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
    fun `home screen opening interface displays creator credit`() {
        composeRule.setContent {
            DotsAndBoxesTheme {
                HomeScreen(
                    settings = UserSettings(),
                    onUpdateNickname = {},
                    onToggleSound = {},
                    onToggleVibration = {},
                    onCreateRoom = {},
                    onJoinRoom = {},
                    onPracticeAi = {},
                    onHowToPlay = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_creator_credit").assertIsDisplayed()
        composeRule.onNodeWithText("Created By Raihan Kabir").assertIsDisplayed()
    }

    @Test
    fun `play menu displays 1 Play With AI and 2 On Board Multiplayer with 2 to 5 player options`() {
        var aiClicked = false
        val selectedCounts = mutableListOf<Int?>()
        composeRule.setContent {
            DotsAndBoxesTheme {
                PlayMenuScreen(
                    onWithAi = { aiClicked = true },
                    onOnBoardMultiplayer = { count -> selectedCounts.add(count) },
                    onBack = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("1. Play With AI").assertIsDisplayed()
        composeRule.onNodeWithTag("play_menu_with_ai").performClick()
        assertTrue(aiClicked)

        composeRule.onNodeWithText("2. On Board Multiplayer").performScrollTo().assertIsDisplayed()
        for (count in 2..5) {
            composeRule.onNodeWithTag("play_menu_on_board_${count}p").performScrollTo().performClick()
        }
        assertEquals(listOf(2, 3, 4, 5), selectedCounts)
    }

    @Test
    fun `on board setup defaults to 2 players and starts match on button tap`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = GameViewModel(app)
        assertEquals(2, vm.onBoardPlayerCount.value)
        assertEquals(550L, GameViewModel.AI_TURN_DELAY_MS)

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
    fun `viewmodel on board setup supports 2 to 10 local human players with default and custom names`() {
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
                assertTrue("On Board player must not be AI", !state.players[idx].isAi)
            }

            // First local human player draws a non-scoring line; turn advances manually to player 2 (index 1)
            vm.submitLineMove(LineId(LineType.HORIZONTAL, 0, 0))
            val afterMove = vm.onBoardGameState.value!!
            assertEquals(1, afterMove.currentTurnIndex)
            assertEquals(1, DotsAndBoxesEngine.availableLines(state).size - DotsAndBoxesEngine.availableLines(afterMove).size)
        }
    }

    @Test
    fun `join room screen shows discovered nearby rooms and connects on tap`() {
        val room = DiscoveredRoom(
            hostId = "host_abc",
            hostName = "Raihan",
            hostIp = "192.168.183.74",
            port = 45678,
            boardSize = BoardSize.SMALL,
            playerCount = 1,
            maxPlayers = 4,
        )
        var connectedIp: String? = null
        var connectedPort: String? = null

        composeRule.setContent {
            DotsAndBoxesTheme {
                JoinRoomScreen(
                    clientSnapshot = ClientSessionSnapshot(),
                    initialNickname = "Friend",
                    lastHostIp = "192.168.43.1",
                    detectedGatewayIp = "192.168.183.74",
                    discoveredRooms = listOf(room),
                    onNicknameChange = {},
                    onConnect = { ip, port ->
                        connectedIp = ip
                        connectedPort = port
                    },
                    onBack = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Nearby Rooms Found").assertIsDisplayed()
        composeRule.onNodeWithText("Raihan's Room").assertIsDisplayed()
        composeRule.onNodeWithTag("discovered_room_join_host_abc").performClick()
        assertEquals("192.168.183.74", connectedIp)
        assertEquals("45678", connectedPort)
    }

    @Test
    fun `board draws horizontal and vertical lines on drag and ignores plain taps`() {
        val initialState = DotsAndBoxesEngine.createInitialState(
            boardSize = BoardSize.TINY,
            players = listOf(
                PlayerInfo(id = "p1", name = "Player 1", colorIndex = 0),
                PlayerInfo(id = "p2", name = "Player 2", colorIndex = 1),
            ),
        )
        // Pre-draw horizontal (1, 0) so we can also test dragging an already-drawn line
        val preDrawnLine = LineId(LineType.HORIZONTAL, 1, 0)
        val gameState = (DotsAndBoxesEngine.applyMove(initialState, "p1", preDrawnLine) as MoveResult.Success).newState

        val committedLines = mutableListOf<LineId>()
        var invalidFeedbackCount = 0

        composeRule.setContent {
            DotsAndBoxesTheme {
                DotsAndBoxesBoard(
                    gameState = gameState,
                    localPlayerId = "p1",
                    isMyTurn = true,
                    onDotSelectedFeedback = {},
                    onInvalidTapFeedback = { invalidFeedbackCount++ },
                    onLineCommitted = { committedLines.add(it) },
                    modifier = Modifier.size(320.dp),
                )
            }
        }
        composeRule.waitForIdle()

        val dots = BoardSize.TINY.dots

        fun dotCenter(w: Int, h: Int, row: Int, col: Int): Offset {
            val canvasMin = minOf(w.toFloat(), h.toFloat())
            val padding = (canvasMin * 0.085f).coerceIn(22f, 52f)
            val gridSpan = (canvasMin - padding * 2f).coerceAtLeast(1f)
            val step = gridSpan / (dots - 1).coerceAtLeast(1)
            val originX = (w - gridSpan) / 2f
            val originY = (h - gridSpan) / 2f
            return Offset(originX + col * step, originY + row * step)
        }

        // 1. Plain taps on two adjacent dots must NOT draw a line
        composeRule.onNodeWithTag("game_board_canvas").performTouchInput {
            val d00 = dotCenter(width, height, 0, 0)
            val d01 = dotCenter(width, height, 0, 1)
            down(d00)
            advanceEventTime(16L)
            up()
            advanceEventTime(40L)
            down(d01)
            advanceEventTime(16L)
            up()
        }
        composeRule.waitForIdle()
        assertTrue("Tapping dots without dragging must not draw a line", committedLines.isEmpty())

        // 2. Drag from (0, 0) to (0, 1) commits HORIZONTAL(0, 0)
        composeRule.onNodeWithTag("game_board_canvas").performTouchInput {
            val start = dotCenter(width, height, 0, 0)
            val end = dotCenter(width, height, 0, 1)
            val mid = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
            down(start)
            advanceEventTime(16L)
            moveTo(mid)
            advanceEventTime(16L)
            moveTo(end)
            advanceEventTime(16L)
            up()
        }
        composeRule.waitForIdle()
        assertEquals(listOf(LineId(LineType.HORIZONTAL, 0, 0)), committedLines)

        // 3. Drag from (0, 0) to (1, 0) commits VERTICAL(0, 0)
        composeRule.onNodeWithTag("game_board_canvas").performTouchInput {
            val start = dotCenter(width, height, 0, 0)
            val end = dotCenter(width, height, 1, 0)
            val mid = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
            down(start)
            advanceEventTime(16L)
            moveTo(mid)
            advanceEventTime(16L)
            moveTo(end)
            advanceEventTime(16L)
            up()
        }
        composeRule.waitForIdle()
        assertEquals(
            listOf(
                LineId(LineType.HORIZONTAL, 0, 0),
                LineId(LineType.VERTICAL, 0, 0),
            ),
            committedLines,
        )

        // 4. Dragging along an already-drawn line triggers invalid feedback and does not commit
        composeRule.onNodeWithTag("game_board_canvas").performTouchInput {
            val start = dotCenter(width, height, 1, 0)
            val end = dotCenter(width, height, 1, 1)
            val mid = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
            down(start)
            advanceEventTime(16L)
            moveTo(mid)
            advanceEventTime(16L)
            moveTo(end)
            advanceEventTime(16L)
            up()
        }
        composeRule.waitForIdle()
        assertEquals(2, committedLines.size)
        assertEquals(1, invalidFeedbackCount)
    }
}
