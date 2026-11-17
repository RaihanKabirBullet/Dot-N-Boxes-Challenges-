package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.model.GameMode
import com.example.ui.GameViewModel
import com.example.ui.UiNavEvent
import com.example.ui.navigation.NavRoutes
import com.example.ui.screens.BoardSelectionScreen
import com.example.ui.screens.CreateRoomScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HowToPlayScreen
import com.example.ui.screens.JoinRoomScreen
import com.example.ui.screens.LobbyScreen
import com.example.ui.screens.OnBoardSetupScreen
import com.example.ui.screens.PauseScreen
import com.example.ui.screens.PlayMenuScreen
import com.example.ui.screens.ResultsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.DotsAndBoxesTheme
import com.example.ui.theme.NavyCardElevated
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            DotsAndBoxesTheme {
                DotsAndBoxesApp()
            }
        }
    }
}

@Composable
fun DotsAndBoxesApp(
    viewModel: GameViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val hostSnapshot by viewModel.hostSnapshot.collectAsStateWithLifecycle()
    val clientSnapshot by viewModel.clientSnapshot.collectAsStateWithLifecycle()
    val activeMode by viewModel.activeMode.collectAsStateWithLifecycle()
    val practiceBoardSize by viewModel.practiceBoardSize.collectAsStateWithLifecycle()
    val practiceAiCount by viewModel.practiceAiCount.collectAsStateWithLifecycle()
    val practiceGameState by viewModel.practiceGameState.collectAsStateWithLifecycle()
    val onBoardPlayerCount by viewModel.onBoardPlayerCount.collectAsStateWithLifecycle()
    val onBoardBoardSize by viewModel.onBoardBoardSize.collectAsStateWithLifecycle()
    val onBoardPlayerNames by viewModel.onBoardPlayerNames.collectAsStateWithLifecycle()
    val onBoardGameState by viewModel.onBoardGameState.collectAsStateWithLifecycle()
    val localPlayerId by viewModel.localPlayerId.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                UiNavEvent.NavigateToLobby -> {
                    navController.navigate(NavRoutes.LOBBY) {
                        popUpTo(NavRoutes.HOME)
                    }
                }
                UiNavEvent.NavigateToGame -> {
                    navController.navigate(NavRoutes.GAME) {
                        popUpTo(NavRoutes.HOME)
                    }
                }
                UiNavEvent.NavigateToResults -> {
                    if (navController.currentDestination?.route != NavRoutes.RESULTS) {
                        navController.navigate(NavRoutes.RESULTS) {
                            popUpTo(NavRoutes.GAME) { inclusive = true }
                        }
                    }
                }
                UiNavEvent.NavigateToHome -> {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                    }
                }
                is UiNavEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.text)
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = NavyCardElevated,
                    contentColor = TextPrimary,
                    actionColor = AccentCyan,
                )
            }
        },
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = NavRoutes.SPLASH,
            ) {
                composable(NavRoutes.SPLASH) {
                    SplashScreen(
                        onFinished = {
                            navController.navigate(NavRoutes.HOME) {
                                popUpTo(NavRoutes.SPLASH) { inclusive = true }
                            }
                        },
                    )
                }

                composable(NavRoutes.HOME) {
                    HomeScreen(
                        settings = settings,
                        onUpdateNickname = viewModel::updateNickname,
                        onToggleSound = viewModel::toggleSound,
                        onToggleVibration = viewModel::toggleVibration,
                        onCreateRoom = { navController.navigate(NavRoutes.CREATE_ROOM) },
                        onJoinRoom = { navController.navigate(NavRoutes.JOIN_ROOM) },
                        onPracticeAi = { navController.navigate(NavRoutes.PLAY_MENU) },
                        onHowToPlay = { navController.navigate(NavRoutes.HOW_TO_PLAY) },
                    )
                }

                composable(NavRoutes.PLAY_MENU) {
                    PlayMenuScreen(
                        onWithAi = { navController.navigate(NavRoutes.boardSelection("practice")) },
                        onOnBoardMultiplayer = { navController.navigate(NavRoutes.ON_BOARD_SETUP) },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(NavRoutes.ON_BOARD_SETUP) {
                    OnBoardSetupScreen(
                        playerCount = onBoardPlayerCount,
                        selectedBoardSize = onBoardBoardSize,
                        playerNames = onBoardPlayerNames,
                        defaultNameProvider = viewModel::defaultOnBoardPlayerName,
                        onPlayerCountChange = viewModel::setOnBoardPlayerCount,
                        onBoardSizeChange = viewModel::setOnBoardBoardSize,
                        onPlayerNameChange = viewModel::setOnBoardPlayerName,
                        onStartGame = viewModel::startOnBoardMatch,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(NavRoutes.CREATE_ROOM) {
                    CreateRoomScreen(
                        hostSnapshot = hostSnapshot,
                        initialNickname = settings.nickname,
                        onStartHosting = { viewModel.startHostingRoom() },
                        onRefreshIp = viewModel::refreshHostIp,
                        onNicknameChange = viewModel::updateNickname,
                        onSelectBoardSize = viewModel::updateHostBoardSize,
                        onEnterLobby = { navController.navigate(NavRoutes.LOBBY) },
                        onCopiedIp = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Host IP copied to clipboard")
                            }
                        },
                        onBack = {
                            viewModel.leaveRoomAndReset()
                            navController.popBackStack()
                        },
                    )
                }

                composable(NavRoutes.JOIN_ROOM) {
                    JoinRoomScreen(
                        clientSnapshot = clientSnapshot,
                        initialNickname = settings.nickname,
                        lastHostIp = settings.lastHostIp,
                        onNicknameChange = viewModel::updateNickname,
                        onConnect = viewModel::connectToHost,
                        onBack = {
                            viewModel.leaveRoomAndReset()
                            navController.popBackStack()
                        },
                    )
                }

                composable(NavRoutes.LOBBY) {
                    LobbyScreen(
                        mode = activeMode,
                        localPlayerId = localPlayerId,
                        hostSnapshot = hostSnapshot,
                        clientSnapshot = clientSnapshot,
                        onRefreshHostIp = viewModel::refreshHostIp,
                        onOpenBoardSelection = {
                            navController.navigate(NavRoutes.boardSelection("host"))
                        },
                        onKickPlayer = viewModel::kickPlayer,
                        onStartMatch = viewModel::startMultiplayerMatch,
                        onCopiedIp = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Host IP copied to clipboard")
                            }
                        },
                        onLeaveLobby = {
                            viewModel.leaveRoomAndReset()
                            navController.navigate(NavRoutes.HOME) {
                                popUpTo(NavRoutes.HOME) { inclusive = true }
                            }
                        },
                    )
                }

                composable(
                    route = NavRoutes.BOARD_SELECTION,
                    arguments = listOf(navArgument("source") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val source = backStackEntry.arguments?.getString("source") ?: "practice"
                    val isPractice = source == "practice"
                    BoardSelectionScreen(
                        isPracticeMode = isPractice,
                        selectedBoardSize = if (isPractice) practiceBoardSize else hostSnapshot.boardSize,
                        aiCount = practiceAiCount,
                        onSelectBoardSize = { size ->
                            if (isPractice) viewModel.setPracticeBoardSize(size)
                            else viewModel.updateHostBoardSize(size)
                        },
                        onSelectAiCount = viewModel::setPracticeAiCount,
                        onConfirm = {
                            if (isPractice) {
                                viewModel.startPracticeMatch()
                            } else {
                                navController.popBackStack()
                            }
                        },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(NavRoutes.GAME) {
                    val currentGame = when (activeMode) {
                        GameMode.PRACTICE_AI -> practiceGameState
                        GameMode.ON_BOARD_LOCAL -> onBoardGameState
                        GameMode.HOST_MULTIPLAYER -> hostSnapshot.gameState
                        GameMode.CLIENT_MULTIPLAYER -> clientSnapshot.gameState
                    }
                    GameScreen(
                        mode = activeMode,
                        gameState = currentGame,
                        localPlayerId = localPlayerId,
                        soundEnabled = settings.soundEnabled,
                        clientStatus = clientSnapshot.status,
                        onToggleSound = viewModel::toggleSound,
                        onPause = {
                            viewModel.pauseGame()
                            navController.navigate(NavRoutes.PAUSE)
                        },
                        onDotSelectedFeedback = { viewModel.feedbackManager.onDotSelected() },
                        onInvalidTapFeedback = { viewModel.feedbackManager.onError() },
                        onLineCommitted = viewModel::submitLineMove,
                        onReconnectClient = viewModel::reconnectClient,
                    )
                }

                composable(NavRoutes.PAUSE) {
                    PauseScreen(
                        mode = activeMode,
                        settings = settings,
                        onToggleSound = viewModel::toggleSound,
                        onToggleVibration = viewModel::toggleVibration,
                        onResume = {
                            viewModel.resumeGame()
                            navController.popBackStack()
                        },
                        onRestart = {
                            navController.popBackStack()
                            viewModel.restartCurrentMatch()
                        },
                        onHowToPlay = { navController.navigate(NavRoutes.HOW_TO_PLAY) },
                        onReturnHome = {
                            viewModel.leaveRoomAndReset()
                            navController.navigate(NavRoutes.HOME) {
                                popUpTo(NavRoutes.HOME) { inclusive = true }
                            }
                        },
                    )
                }

                composable(NavRoutes.RESULTS) {
                    val currentGame = when (activeMode) {
                        GameMode.PRACTICE_AI -> practiceGameState
                        GameMode.ON_BOARD_LOCAL -> onBoardGameState
                        GameMode.HOST_MULTIPLAYER -> hostSnapshot.gameState
                        GameMode.CLIENT_MULTIPLAYER -> clientSnapshot.gameState
                    }
                    ResultsScreen(
                        mode = activeMode,
                        gameState = currentGame,
                        localPlayerId = localPlayerId,
                        onPlayAgain = { viewModel.restartCurrentMatch() },
                        onBackToLobby = { viewModel.returnToLobbyFromResults() },
                        onReturnHome = {
                            viewModel.leaveRoomAndReset()
                            navController.navigate(NavRoutes.HOME) {
                                popUpTo(NavRoutes.HOME) { inclusive = true }
                            }
                        },
                    )
                }

                composable(NavRoutes.HOW_TO_PLAY) {
                    HowToPlayScreen(
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
