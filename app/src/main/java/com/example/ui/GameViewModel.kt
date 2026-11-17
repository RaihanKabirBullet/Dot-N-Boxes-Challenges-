package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SettingsRepository
import com.example.data.UserSettings
import com.example.feedback.FeedbackManager
import com.example.game.DotsAndBoxesEngine
import com.example.game.MoveResult
import com.example.game.SimpleAiPlayer
import com.example.model.BoardSize
import com.example.model.GameMode
import com.example.model.GamePhase
import com.example.model.GameState
import com.example.model.LineId
import com.example.model.PlayerInfo
import com.example.network.ClientConnectionStatus
import com.example.network.ClientEvent
import com.example.network.ClientSessionSnapshot
import com.example.network.HostRoomSnapshot
import com.example.network.NetworkProtocol
import com.example.network.TcpClientSession
import com.example.network.TcpHostServer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiNavEvent {
    data object NavigateToLobby : UiNavEvent
    data object NavigateToGame : UiNavEvent
    data object NavigateToResults : UiNavEvent
    data object NavigateToHome : UiNavEvent
    data class ShowMessage(val text: String) : UiNavEvent
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    val feedbackManager = FeedbackManager(application)

    private val hostServer = TcpHostServer()
    private val clientSession = TcpClientSession()

    val settings: StateFlow<UserSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserSettings(),
        )

    val hostSnapshot: StateFlow<HostRoomSnapshot> = hostServer.snapshot
    val clientSnapshot: StateFlow<ClientSessionSnapshot> = clientSession.snapshot

    private val _activeMode = MutableStateFlow(GameMode.PRACTICE_AI)
    val activeMode: StateFlow<GameMode> = _activeMode.asStateFlow()

    private val _practiceBoardSize = MutableStateFlow(BoardSize.SMALL)
    val practiceBoardSize: StateFlow<BoardSize> = _practiceBoardSize.asStateFlow()

    private val _practiceAiCount = MutableStateFlow(1)
    val practiceAiCount: StateFlow<Int> = _practiceAiCount.asStateFlow()

    private val _practiceGameState = MutableStateFlow<GameState?>(null)
    val practiceGameState: StateFlow<GameState?> = _practiceGameState.asStateFlow()

    private val _onBoardPlayerCount = MutableStateFlow(2)
    val onBoardPlayerCount: StateFlow<Int> = _onBoardPlayerCount.asStateFlow()

    private val _onBoardBoardSize = MutableStateFlow(BoardSize.SMALL)
    val onBoardBoardSize: StateFlow<BoardSize> = _onBoardBoardSize.asStateFlow()

    private val _onBoardPlayerNames = MutableStateFlow(List(10) { "" })
    val onBoardPlayerNames: StateFlow<List<String>> = _onBoardPlayerNames.asStateFlow()

    private val _onBoardGameState = MutableStateFlow<GameState?>(null)
    val onBoardGameState: StateFlow<GameState?> = _onBoardGameState.asStateFlow()

    private val _localPlayerId = MutableStateFlow("")
    val localPlayerId: StateFlow<String> = _localPlayerId.asStateFlow()

    private val _navEvents = MutableSharedFlow<UiNavEvent>(extraBufferCapacity = 16)
    val navEvents: SharedFlow<UiNavEvent> = _navEvents.asSharedFlow()

    private var aiTurnJob: Job? = null
    private var previousHostSequence: Long = -1L
    private var previousClientSequence: Long = -1L
    private var lastObservedFinishedSeq: Long = -1L

    init {
        viewModelScope.launch {
            val pid = settingsRepository.ensureLocalPlayerId()
            _localPlayerId.value = pid
        }

        viewModelScope.launch {
            settings.collect { userSettings ->
                feedbackManager.soundEnabled = userSettings.soundEnabled
                feedbackManager.vibrationEnabled = userSettings.vibrationEnabled
            }
        }

        hostServer.setEventListener { event ->
            when (event) {
                TcpHostServer.HostGameEvent.LINE_DRAWN -> feedbackManager.onLineDrawn()
                TcpHostServer.HostGameEvent.BOX_CLAIMED -> feedbackManager.onBoxClaimed(isBonus = false)
                TcpHostServer.HostGameEvent.BONUS_TURN -> feedbackManager.onBoxClaimed(isBonus = true)
                TcpHostServer.HostGameEvent.GAME_FINISHED -> feedbackManager.onVictory()
                TcpHostServer.HostGameEvent.PLAYER_JOINED -> feedbackManager.onDotSelected()
                TcpHostServer.HostGameEvent.PLAYER_LEFT -> Unit
            }
        }

        viewModelScope.launch {
            clientSession.events.collect { event ->
                when (event) {
                    ClientEvent.JoinedLobby -> {
                        _activeMode.value = GameMode.CLIENT_MULTIPLAYER
                        _navEvents.tryEmit(UiNavEvent.NavigateToLobby)
                    }
                    ClientEvent.MatchStarted -> {
                        previousClientSequence = -1L
                        lastObservedFinishedSeq = -1L
                        _navEvents.tryEmit(UiNavEvent.NavigateToGame)
                    }
                    is ClientEvent.ErrorBanner -> {
                        feedbackManager.onError()
                        _navEvents.tryEmit(UiNavEvent.ShowMessage(event.message))
                    }
                    is ClientEvent.Disconnected -> {
                        _navEvents.tryEmit(UiNavEvent.ShowMessage(event.reason))
                    }
                }
            }
        }

        // Observe host game state transitions (e.g., finished)
        viewModelScope.launch {
            hostSnapshot.collect { snap ->
                val gs = snap.gameState ?: return@collect
                if (_activeMode.value == GameMode.HOST_MULTIPLAYER) {
                    if (gs.phase == GamePhase.FINISHED && gs.moveSequence != lastObservedFinishedSeq) {
                        lastObservedFinishedSeq = gs.moveSequence
                        delay(700L)
                        _navEvents.tryEmit(UiNavEvent.NavigateToResults)
                    }
                    previousHostSequence = gs.moveSequence
                }
            }
        }

        // Observe client game state transitions for sound/haptics & results
        viewModelScope.launch {
            clientSnapshot.collect { snap ->
                val gs = snap.gameState ?: return@collect
                if (_activeMode.value == GameMode.CLIENT_MULTIPLAYER) {
                    if (gs.moveSequence > previousClientSequence && previousClientSequence >= 0L) {
                        when {
                            gs.phase == GamePhase.FINISHED -> feedbackManager.onVictory()
                            gs.bonusTurnAwarded -> feedbackManager.onBoxClaimed(isBonus = true)
                            gs.lastClaimedBoxIndices.isNotEmpty() -> feedbackManager.onBoxClaimed(isBonus = false)
                            else -> feedbackManager.onLineDrawn()
                        }
                    }
                    previousClientSequence = gs.moveSequence
                    if (gs.phase == GamePhase.FINISHED && gs.moveSequence != lastObservedFinishedSeq) {
                        lastObservedFinishedSeq = gs.moveSequence
                        delay(700L)
                        _navEvents.tryEmit(UiNavEvent.NavigateToResults)
                    }
                }
            }
        }
    }

    fun updateNickname(name: String) {
        viewModelScope.launch {
            settingsRepository.setNickname(name)
            if (_activeMode.value == GameMode.HOST_MULTIPLAYER && hostSnapshot.value.isRunning) {
                hostServer.updateHostName(name)
            }
        }
    }

    fun toggleSound() {
        viewModelScope.launch {
            settingsRepository.setSoundEnabled(!settings.value.soundEnabled)
        }
    }

    fun toggleVibration() {
        viewModelScope.launch {
            settingsRepository.setVibrationEnabled(!settings.value.vibrationEnabled)
        }
    }

    fun setPracticeBoardSize(size: BoardSize) {
        _practiceBoardSize.value = size
    }

    fun setPracticeAiCount(count: Int) {
        _practiceAiCount.value = count.coerceIn(1, 3)
    }

    fun startPracticeMatch() {
        aiTurnJob?.cancel()
        viewModelScope.launch {
            val pid = _localPlayerId.value.ifBlank { settingsRepository.ensureLocalPlayerId() }
            _localPlayerId.value = pid
            val nick = settings.value.nickname.trim().ifEmpty { "You" }
            val aiCount = _practiceAiCount.value.coerceIn(1, 3)
            val players = buildList {
                add(PlayerInfo(id = pid, name = nick, colorIndex = 0, isHost = true, isAi = false))
                val aiNames = listOf("Nova Bot", "Pulse Bot", "Vortex Bot")
                for (i in 1..aiCount) {
                    add(
                        PlayerInfo(
                            id = "ai_$i",
                            name = aiNames[i - 1],
                            colorIndex = i,
                            isHost = false,
                            isAi = true,
                        ),
                    )
                }
            }
            val initial = DotsAndBoxesEngine.createInitialState(_practiceBoardSize.value, players)
            _activeMode.value = GameMode.PRACTICE_AI
            lastObservedFinishedSeq = -1L
            _practiceGameState.value = initial
            _navEvents.tryEmit(UiNavEvent.NavigateToGame)
        }
    }

    fun defaultOnBoardPlayerName(index: Int): String {
        val letters = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")
        val letter = letters.getOrElse(index) { (index + 1).toString() }
        return "$letter Player"
    }

    fun setOnBoardPlayerCount(count: Int) {
        _onBoardPlayerCount.value = count.coerceIn(2, 10)
    }

    fun setOnBoardBoardSize(size: BoardSize) {
        _onBoardBoardSize.value = size
    }

    fun setOnBoardPlayerName(index: Int, name: String) {
        if (index !in 0 until 10) return
        val sanitized = name.take(18)
        _onBoardPlayerNames.value = _onBoardPlayerNames.value.mapIndexed { idx, existing ->
            if (idx == index) sanitized else existing
        }
    }

    fun startOnBoardMatch() {
        aiTurnJob?.cancel()
        val count = _onBoardPlayerCount.value.coerceIn(2, 10)
        val names = _onBoardPlayerNames.value
        val players = (0 until count).map { idx ->
            val raw = names.getOrNull(idx).orEmpty().trim()
            val resolvedName = raw.ifEmpty { defaultOnBoardPlayerName(idx) }
            PlayerInfo(
                id = "onboard_${idx + 1}",
                name = resolvedName,
                colorIndex = idx,
                isHost = idx == 0,
                isAi = false,
                isConnected = true,
                score = 0,
            )
        }
        val initial = DotsAndBoxesEngine.createInitialState(_onBoardBoardSize.value, players)
        _activeMode.value = GameMode.ON_BOARD_LOCAL
        lastObservedFinishedSeq = -1L
        _onBoardGameState.value = initial
        _navEvents.tryEmit(UiNavEvent.NavigateToGame)
    }

    fun startHostingRoom(initialBoardSize: BoardSize = BoardSize.SMALL) {
        viewModelScope.launch {
            val pid = _localPlayerId.value.ifBlank { settingsRepository.ensureLocalPlayerId() }
            _localPlayerId.value = pid
            _activeMode.value = GameMode.HOST_MULTIPLAYER
            val result = hostServer.startServer(
                hostId = pid,
                hostName = settings.value.nickname,
                initialBoardSize = initialBoardSize,
            )
            if (result.isFailure) {
                _navEvents.tryEmit(
                    UiNavEvent.ShowMessage(result.exceptionOrNull()?.message ?: "Could not start room server."),
                )
            }
        }
    }

    fun refreshHostIp() {
        hostServer.refreshHostIp()
    }

    fun updateHostBoardSize(size: BoardSize) {
        viewModelScope.launch {
            hostServer.updateBoardSize(size)
        }
    }

    fun kickPlayer(playerId: String) {
        viewModelScope.launch {
            hostServer.removePlayer(playerId)
        }
    }

    fun startMultiplayerMatch() {
        viewModelScope.launch {
            lastObservedFinishedSeq = -1L
            previousHostSequence = -1L
            val result = hostServer.startMatch()
            if (result.isSuccess) {
                _navEvents.tryEmit(UiNavEvent.NavigateToGame)
            } else {
                feedbackManager.onError()
                _navEvents.tryEmit(
                    UiNavEvent.ShowMessage(result.exceptionOrNull()?.message ?: "Unable to start match."),
                )
            }
        }
    }

    fun connectToHost(hostIp: String, portText: String) {
        val cleanIp = hostIp.trim()
        if (cleanIp.isEmpty()) {
            feedbackManager.onError()
            _navEvents.tryEmit(UiNavEvent.ShowMessage("Please enter the Host IPv4 address."))
            return
        }
        val port = portText.trim().toIntOrNull()?.takeIf { it in 1024..65535 } ?: NetworkProtocol.DEFAULT_PORT
        viewModelScope.launch {
            val pid = _localPlayerId.value.ifBlank { settingsRepository.ensureLocalPlayerId() }
            _localPlayerId.value = pid
            settingsRepository.setLastHostIp(cleanIp)
            _activeMode.value = GameMode.CLIENT_MULTIPLAYER
            clientSession.connect(
                hostIp = cleanIp,
                port = port,
                playerId = pid,
                playerName = settings.value.nickname,
            )
        }
    }

    fun reconnectClient() {
        val snap = clientSnapshot.value
        if (snap.hostIp.isBlank()) return
        viewModelScope.launch {
            val pid = _localPlayerId.value.ifBlank { settingsRepository.ensureLocalPlayerId() }
            clientSession.connect(
                hostIp = snap.hostIp,
                port = snap.port,
                playerId = pid,
                playerName = settings.value.nickname,
            )
        }
    }

    fun submitLineMove(line: LineId) {
        val pid = _localPlayerId.value
        when (_activeMode.value) {
            GameMode.PRACTICE_AI -> {
                val gs = _practiceGameState.value ?: return
                val result = DotsAndBoxesEngine.applyMove(gs, pid, line)
                handlePracticeMoveResult(result)
            }
            GameMode.ON_BOARD_LOCAL -> {
                val gs = _onBoardGameState.value ?: return
                val activeId = gs.currentPlayer?.id ?: return
                val result = DotsAndBoxesEngine.applyMove(gs, activeId, line)
                handleOnBoardMoveResult(result)
            }
            GameMode.HOST_MULTIPLAYER -> {
                viewModelScope.launch {
                    val result = hostServer.submitHostMove(pid, line)
                    if (result is MoveResult.Invalid) {
                        feedbackManager.onError()
                        _navEvents.tryEmit(UiNavEvent.ShowMessage(result.reason))
                    }
                }
            }
            GameMode.CLIENT_MULTIPLAYER -> {
                val snap = clientSnapshot.value
                val gs = snap.gameState ?: return
                if (snap.status != ClientConnectionStatus.IN_GAME) {
                    feedbackManager.onError()
                    _navEvents.tryEmit(UiNavEvent.ShowMessage("Not connected to host."))
                    return
                }
                if (gs.currentPlayer?.id != pid) {
                    feedbackManager.onError()
                    _navEvents.tryEmit(UiNavEvent.ShowMessage("Wait for ${gs.currentPlayer?.name ?: "opponent"}'s turn."))
                    return
                }
                if (DotsAndBoxesEngine.isLineDrawn(gs, line)) {
                    feedbackManager.onError()
                    _navEvents.tryEmit(UiNavEvent.ShowMessage("That line is already drawn."))
                    return
                }
                clientSession.sendMove(pid, line)
            }
        }
    }

    private fun handleOnBoardMoveResult(result: MoveResult) {
        when (result) {
            is MoveResult.Invalid -> {
                feedbackManager.onError()
                _navEvents.tryEmit(UiNavEvent.ShowMessage(result.reason))
            }
            is MoveResult.Success -> {
                _onBoardGameState.value = result.newState
                when {
                    result.gameFinished -> feedbackManager.onVictory()
                    result.grantedBonusTurn -> feedbackManager.onBoxClaimed(isBonus = true)
                    result.claimedCount > 0 -> feedbackManager.onBoxClaimed(isBonus = false)
                    else -> feedbackManager.onLineDrawn()
                }
                if (result.gameFinished) {
                    viewModelScope.launch {
                        delay(700L)
                        _navEvents.tryEmit(UiNavEvent.NavigateToResults)
                    }
                }
            }
        }
    }

    private fun handlePracticeMoveResult(result: MoveResult) {
        when (result) {
            is MoveResult.Invalid -> {
                feedbackManager.onError()
                _navEvents.tryEmit(UiNavEvent.ShowMessage(result.reason))
            }
            is MoveResult.Success -> {
                _practiceGameState.value = result.newState
                when {
                    result.gameFinished -> feedbackManager.onVictory()
                    result.grantedBonusTurn -> feedbackManager.onBoxClaimed(isBonus = true)
                    result.claimedCount > 0 -> feedbackManager.onBoxClaimed(isBonus = false)
                    else -> feedbackManager.onLineDrawn()
                }
                if (result.gameFinished) {
                    viewModelScope.launch {
                        delay(700L)
                        _navEvents.tryEmit(UiNavEvent.NavigateToResults)
                    }
                } else {
                    maybeScheduleAiTurn(result.newState)
                }
            }
        }
    }

    private fun maybeScheduleAiTurn(state: GameState) {
        if (state.phase != GamePhase.IN_PROGRESS) return
        val current = state.currentPlayer ?: return
        if (!current.isAi) return

        aiTurnJob?.cancel()
        aiTurnJob = viewModelScope.launch {
            delay(440L)
            val latest = _practiceGameState.value ?: return@launch
            if (latest.phase != GamePhase.IN_PROGRESS) return@launch
            val activePlayer = latest.currentPlayer ?: return@launch
            if (!activePlayer.isAi) return@launch

            val move = SimpleAiPlayer.chooseMove(latest) ?: return@launch
            val result = DotsAndBoxesEngine.applyMove(latest, activePlayer.id, move)
            handlePracticeMoveResult(result)
        }
    }

    fun pauseGame() {
        when (_activeMode.value) {
            GameMode.PRACTICE_AI -> {
                aiTurnJob?.cancel()
                val gs = _practiceGameState.value ?: return
                if (gs.phase == GamePhase.IN_PROGRESS) {
                    _practiceGameState.value = gs.copy(phase = GamePhase.PAUSED)
                }
            }
            GameMode.ON_BOARD_LOCAL -> {
                val gs = _onBoardGameState.value ?: return
                if (gs.phase == GamePhase.IN_PROGRESS) {
                    _onBoardGameState.value = gs.copy(phase = GamePhase.PAUSED)
                }
            }
            GameMode.HOST_MULTIPLAYER -> {
                viewModelScope.launch { hostServer.setPaused(true) }
            }
            GameMode.CLIENT_MULTIPLAYER -> Unit
        }
    }

    fun resumeGame() {
        when (_activeMode.value) {
            GameMode.PRACTICE_AI -> {
                val gs = _practiceGameState.value ?: return
                if (gs.phase == GamePhase.PAUSED) {
                    val resumed = gs.copy(phase = GamePhase.IN_PROGRESS)
                    _practiceGameState.value = resumed
                    maybeScheduleAiTurn(resumed)
                }
            }
            GameMode.ON_BOARD_LOCAL -> {
                val gs = _onBoardGameState.value ?: return
                if (gs.phase == GamePhase.PAUSED) {
                    _onBoardGameState.value = gs.copy(phase = GamePhase.IN_PROGRESS)
                }
            }
            GameMode.HOST_MULTIPLAYER -> {
                viewModelScope.launch { hostServer.setPaused(false) }
            }
            GameMode.CLIENT_MULTIPLAYER -> Unit
        }
    }

    fun restartCurrentMatch() {
        when (_activeMode.value) {
            GameMode.PRACTICE_AI -> startPracticeMatch()
            GameMode.ON_BOARD_LOCAL -> startOnBoardMatch()
            GameMode.HOST_MULTIPLAYER -> {
                viewModelScope.launch {
                    lastObservedFinishedSeq = -1L
                    previousHostSequence = -1L
                    val res = hostServer.restartMatch()
                    if (res.isSuccess) {
                        _navEvents.tryEmit(UiNavEvent.NavigateToGame)
                    } else {
                        _navEvents.tryEmit(
                            UiNavEvent.ShowMessage(res.exceptionOrNull()?.message ?: "Need at least 2 players to restart."),
                        )
                    }
                }
            }
            GameMode.CLIENT_MULTIPLAYER -> {
                _navEvents.tryEmit(UiNavEvent.ShowMessage("Only the host can restart a multiplayer match."))
            }
        }
    }

    fun returnToLobbyFromResults() {
        when (_activeMode.value) {
            GameMode.HOST_MULTIPLAYER -> {
                viewModelScope.launch {
                    hostServer.returnToLobby()
                    _navEvents.tryEmit(UiNavEvent.NavigateToLobby)
                }
            }
            GameMode.CLIENT_MULTIPLAYER -> {
                _navEvents.tryEmit(UiNavEvent.NavigateToLobby)
            }
            GameMode.PRACTICE_AI -> startPracticeMatch()
            GameMode.ON_BOARD_LOCAL -> startOnBoardMatch()
        }
    }

    fun leaveRoomAndReset() {
        aiTurnJob?.cancel()
        _practiceGameState.value = null
        _onBoardGameState.value = null
        viewModelScope.launch {
            hostServer.stopServer()
            clientSession.disconnect()
        }
    }

    fun currentActiveGameState(): GameState? {
        return when (_activeMode.value) {
            GameMode.PRACTICE_AI -> _practiceGameState.value
            GameMode.ON_BOARD_LOCAL -> _onBoardGameState.value
            GameMode.HOST_MULTIPLAYER -> hostSnapshot.value.gameState
            GameMode.CLIENT_MULTIPLAYER -> clientSnapshot.value.gameState
        }
    }

    override fun onCleared() {
        super.onCleared()
        aiTurnJob?.cancel()
        hostServer.close()
        clientSession.close()
    }
}
