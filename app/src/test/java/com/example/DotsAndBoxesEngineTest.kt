package com.example

import com.example.game.DotsAndBoxesEngine
import com.example.game.MoveResult
import com.example.game.SimpleAiPlayer
import com.example.model.BoardSize
import com.example.model.DotCoord
import com.example.model.GamePhase
import com.example.model.LineId
import com.example.model.LineType
import com.example.model.PlayerInfo
import com.example.network.ClientConnectionStatus
import com.example.network.NetworkProtocol
import com.example.network.ProtocolMessage
import com.example.network.TcpClientSession
import com.example.network.TcpHostServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket

class DotsAndBoxesEngineTest {

    private val p1 = PlayerInfo(id = "p1", name = "Alice", colorIndex = 0, isHost = true)
    private val p2 = PlayerInfo(id = "p2", name = "Bob", colorIndex = 1, isHost = false)

    @Test
    fun `fromDots validates adjacency only`() {
        assertEquals(
            LineId(LineType.HORIZONTAL, 0, 0),
            LineId.fromDots(DotCoord(0, 1), DotCoord(0, 0)),
        )
        assertEquals(
            LineId(LineType.VERTICAL, 1, 2),
            LineId.fromDots(DotCoord(1, 2), DotCoord(2, 2)),
        )
        assertNull(LineId.fromDots(DotCoord(0, 0), DotCoord(1, 1)))
        assertNull(LineId.fromDots(DotCoord(0, 0), DotCoord(0, 2)))
    }

    @Test
    fun `duplicate lines and out of turn moves are rejected`() {
        var state = DotsAndBoxesEngine.createInitialState(BoardSize.TINY, listOf(p1, p2))
        val res1 = DotsAndBoxesEngine.applyMove(state, "p1", LineId(LineType.HORIZONTAL, 0, 0))
        assertTrue(res1 is MoveResult.Success)
        state = (res1 as MoveResult.Success).newState

        // Now it's p2's turn
        val wrongTurn = DotsAndBoxesEngine.applyMove(state, "p1", LineId(LineType.HORIZONTAL, 0, 1))
        assertTrue(wrongTurn is MoveResult.Invalid)
        assertEquals("NOT_YOUR_TURN", (wrongTurn as MoveResult.Invalid).code)

        // Duplicate line by p2
        val duplicate = DotsAndBoxesEngine.applyMove(state, "p2", LineId(LineType.HORIZONTAL, 0, 0))
        assertTrue(duplicate is MoveResult.Invalid)
        assertEquals("DUPLICATE_LINE", (duplicate as MoveResult.Invalid).code)
    }

    @Test
    fun `completing two boxes in one move awards two points and exactly one extra turn`() {
        var state = DotsAndBoxesEngine.createInitialState(BoardSize.TINY, listOf(p1, p2))
        // Pre-draw 3 sides of box (0,0) and box (0,1), leaving shared vertical edge V(0,1)
        val setupMoves = listOf(
            "p1" to LineId(LineType.HORIZONTAL, 0, 0),
            "p2" to LineId(LineType.HORIZONTAL, 1, 0),
            "p1" to LineId(LineType.VERTICAL, 0, 0),
            "p2" to LineId(LineType.HORIZONTAL, 0, 1),
            "p1" to LineId(LineType.HORIZONTAL, 1, 1),
            "p2" to LineId(LineType.VERTICAL, 0, 2),
        )
        for ((pid, line) in setupMoves) {
            state = (DotsAndBoxesEngine.applyMove(state, pid, line) as MoveResult.Success).newState
        }

        // Now it is p1's turn; drawing V(0,1) completes BOTH (0,0) and (0,1)
        val doubleClaim = DotsAndBoxesEngine.applyMove(state, "p1", LineId(LineType.VERTICAL, 0, 1))
        assertTrue(doubleClaim is MoveResult.Success)
        val success = doubleClaim as MoveResult.Success
        assertEquals(2, success.claimedCount)
        assertTrue(success.grantedBonusTurn)
        assertEquals(2, success.newState.players[0].score)
        assertEquals(0, success.newState.currentTurnIndex)

        // On extra turn, p1 draws a non-completing line -> turn must pass to p2 (no banked extra turn!)
        val nextMove = DotsAndBoxesEngine.applyMove(
            success.newState,
            "p1",
            LineId(LineType.HORIZONTAL, 2, 0),
        ) as MoveResult.Success
        assertEquals(0, nextMove.claimedCount)
        assertEquals(1, nextMove.newState.currentTurnIndex)
    }

    @Test
    fun `full 3x3 game finishes cleanly with AI`() {
        var state = DotsAndBoxesEngine.createInitialState(BoardSize.TINY, listOf(p1, p2.copy(isAi = true)))
        var safety = 0
        while (!state.isGameOver && safety < 50) {
            val active = state.currentPlayer!!
            val move = SimpleAiPlayer.chooseMove(state)!!
            state = (DotsAndBoxesEngine.applyMove(state, active.id, move) as MoveResult.Success).newState
            safety++
        }
        assertEquals(GamePhase.FINISHED, state.phase)
        assertEquals(0, state.remainingBoxesCount)
        assertEquals(BoardSize.TINY.totalBoxes, state.players.sumOf { it.score })
    }

    @Test
    fun `network protocol roundtrips all nine message types`() {
        val state = DotsAndBoxesEngine.createInitialState(BoardSize.SMALL, listOf(p1, p2))
        val messages = listOf(
            ProtocolMessage.Join("p1", "Alice"),
            ProtocolMessage.JoinAccepted("p2", 1, BoardSize.MEDIUM, listOf(p1, p2)),
            ProtocolMessage.PlayerList(BoardSize.LARGE, listOf(p1, p2)),
            ProtocolMessage.GameStart(state),
            ProtocolMessage.Move("p1", LineType.VERTICAL, 1, 2),
            ProtocolMessage.GameStateSync(state.copy(moveSequence = 4L)),
            ProtocolMessage.Error("DUPLICATE_LINE", "Already drawn"),
            ProtocolMessage.Ping(123456789L),
            ProtocolMessage.Disconnect("p2", "Left room"),
        )

        for (msg in messages) {
            val encoded = NetworkProtocol.encode(msg)
            assertTrue("Encoded JSON must be single-line", !encoded.contains("\n"))
            val decoded = NetworkProtocol.decode(encoded)
            assertNotNull("Decoded message should not be null for $encoded", decoded)
            assertEquals(msg, decoded)
        }
    }

    @Test
    fun `real tcp host and client synchronize lobby, start, and moves`() = runBlocking {
        val freePort = ServerSocket(0).use { it.localPort }
        val host = TcpHostServer()
        val client = TcpClientSession()

        try {
            val started = host.startServer(
                hostId = "host_1",
                hostName = "HostPlayer",
                initialBoardSize = BoardSize.TINY,
                port = freePort,
            )
            assertTrue(started.isSuccess)

            client.connect(
                hostIp = "127.0.0.1",
                port = freePort,
                playerId = "client_1",
                playerName = "ClientPlayer",
            )

            // Wait for client to join lobby
            var joined = false
            repeat(30) {
                if (client.snapshot.value.status == ClientConnectionStatus.CONNECTED_LOBBY &&
                    host.snapshot.value.players.size == 2
                ) {
                    joined = true
                    return@repeat
                }
                delay(50)
            }
            assertTrue("Client should join host lobby over TCP", joined)

            // Host updates board size and broadcasts
            host.updateBoardSize(BoardSize.HUGE)
            var syncedBoard = false
            repeat(20) {
                if (client.snapshot.value.boardSize == BoardSize.HUGE) {
                    syncedBoard = true
                    return@repeat
                }
                delay(50)
            }
            assertTrue("Client should receive updated board size", syncedBoard)

            // Host starts match
            host.updateBoardSize(BoardSize.TINY)
            val startRes = host.startMatch()
            assertTrue(startRes.isSuccess)

            var gameStarted = false
            repeat(20) {
                if (client.snapshot.value.status == ClientConnectionStatus.IN_GAME) {
                    gameStarted = true
                    return@repeat
                }
                delay(50)
            }
            assertTrue("Client should receive GAME_START", gameStarted)

            // Host makes move 1
            val hostMove = host.submitHostMove("host_1", LineId(LineType.HORIZONTAL, 0, 0))
            assertTrue(hostMove is MoveResult.Success)

            // Wait for client to see move 1 and turn pass to client_1
            var clientTurnSynced = false
            repeat(20) {
                val cgs = client.snapshot.value.gameState
                if (cgs != null && cgs.moveSequence == 1L && cgs.currentPlayer?.id == "client_1") {
                    clientTurnSynced = true
                    return@repeat
                }
                delay(50)
            }
            assertTrue("Client should sync move 1", clientTurnSynced)

            // Client sends move 2 over TCP
            client.sendMove("client_1", LineId(LineType.VERTICAL, 0, 0))
            var hostReceivedClientMove = false
            repeat(20) {
                val hgs = host.snapshot.value.gameState
                if (hgs != null && hgs.moveSequence == 2L) {
                    hostReceivedClientMove = true
                    return@repeat
                }
                delay(50)
            }
            assertTrue("Host should validate and apply client move", hostReceivedClientMove)

            // Verify authoritative lastMove on both host & client matches move 2 by client_1
            var lastMoveSynced = false
            repeat(20) {
                val hlm = host.snapshot.value.gameState?.lastMove
                val clm = client.snapshot.value.gameState?.lastMove
                if (hlm != null && clm != null && hlm == clm &&
                    hlm.playerId == "client_1" &&
                    hlm.lineType == LineType.VERTICAL &&
                    hlm.row == 0 && hlm.col == 0
                ) {
                    lastMoveSynced = true
                    return@repeat
                }
                delay(50)
            }
            assertTrue("Host and client should synchronize lastMove", lastMoveSynced)

            // Reconnect client with same playerId and verify lastMove & ownership remain intact
            client.connect(
                hostIp = "127.0.0.1",
                port = freePort,
                playerId = "client_1",
                playerName = "ClientPlayer",
            )
            var reconnectedWithLastMove = false
            repeat(30) {
                val gs = client.snapshot.value.gameState
                if (client.snapshot.value.status == ClientConnectionStatus.IN_GAME &&
                    gs?.moveSequence == 2L &&
                    gs.lastMove?.playerId == "client_1" &&
                    gs.lastMove?.lineKey == LineId(LineType.VERTICAL, 0, 0).key
                ) {
                    reconnectedWithLastMove = true
                    return@repeat
                }
                delay(50)
            }
            assertTrue("Reconnected client should receive synchronized lastMove", reconnectedWithLastMove)
        } finally {
            client.close()
            host.close()
        }
    }

    @Test
    fun `lastMove tracks most recent move without changing ownership or scores`() {
        var state = DotsAndBoxesEngine.createInitialState(BoardSize.TINY, listOf(p1, p2))
        val m1 = LineId(LineType.HORIZONTAL, 0, 0)
        val m2 = LineId(LineType.HORIZONTAL, 1, 0)
        val m3 = LineId(LineType.VERTICAL, 0, 0)
        val m4 = LineId(LineType.VERTICAL, 0, 1) // p2 completes box (0,0)

        state = (DotsAndBoxesEngine.applyMove(state, "p1", m1) as MoveResult.Success).newState
        assertEquals("p1", state.lastMove?.playerId)
        assertEquals(m1.key, state.lastMove?.lineKey)

        state = (DotsAndBoxesEngine.applyMove(state, "p2", m2) as MoveResult.Success).newState
        assertEquals("p2", state.lastMove?.playerId)
        assertEquals(m2.key, state.lastMove?.lineKey)
        // Original line m1 still owned by p1 (index 0)
        assertEquals(0, DotsAndBoxesEngine.getLineOwner(state, m1))

        state = (DotsAndBoxesEngine.applyMove(state, "p1", m3) as MoveResult.Success).newState
        state = (DotsAndBoxesEngine.applyMove(state, "p2", m4) as MoveResult.Success).newState
        assertEquals("p2", state.lastMove?.playerId)
        assertEquals(m4.key, state.lastMove?.lineKey)
        // Box (0,0) owned by p2 (index 1) and scores unaffected by lastMove highlight
        assertEquals(1, state.boxes[0])
        assertEquals(0, state.players[0].score)
        assertEquals(1, state.players[1].score)
    }

    @Test
    fun `ten player colors are all distinct`() {
        val colors = (0 until 10).map { com.example.model.PlayerColor.fromIndex(it) }
        assertEquals(10, colors.map { it.index }.toSet().size)
        assertEquals(10, colors.map { it.label }.toSet().size)
        assertEquals(10, colors.map { it.primary }.toSet().size)
    }

    @Test
    fun `all player counts from 2 to 10 complete full matches and sum to total boxes`() {
        val defaultLetters = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")
        for (count in 2..10) {
            val players = (0 until count).map { idx ->
                PlayerInfo(
                    id = "onboard_${idx + 1}",
                    name = "${defaultLetters[idx]} Player",
                    colorIndex = idx,
                    isHost = idx == 0,
                )
            }
            var state = DotsAndBoxesEngine.createInitialState(BoardSize.SMALL, players)
            assertEquals(count, state.players.size)

            // Verify sequential turn rotation across all `count` players using non-claiming edge moves first
            var safety = 0
            while (!state.isGameOver && safety < 200) {
                val active = state.currentPlayer!!
                val move = SimpleAiPlayer.chooseMove(state)!!
                val res = DotsAndBoxesEngine.applyMove(state, active.id, move)
                assertTrue("Move should succeed for count=$count", res is MoveResult.Success)
                val success = res as MoveResult.Success
                if (success.claimedCount > 0 && !success.gameFinished) {
                    assertTrue("Claiming box must grant bonus turn for count=$count", success.grantedBonusTurn)
                    assertEquals(active.id, success.newState.currentPlayer?.id)
                }
                state = success.newState
                safety++
            }

            assertEquals("Match must finish for count=$count", GamePhase.FINISHED, state.phase)
            assertEquals("No boxes remaining for count=$count", 0, state.remainingBoxesCount)
            assertEquals(
                "Total scores must equal board boxes for count=$count",
                BoardSize.SMALL.totalBoxes,
                state.players.sumOf { it.score },
            )
            assertEquals(count, state.rankedPlayers.size)
            assertTrue(state.winners.isNotEmpty())
        }
    }

    @Test
    fun `tied scores across multiple players produce multiple winners`() {
        // 3x3 dots = 4 boxes. 4 players can tie at 1 box each, or 2 players tie at 2 each.
        val players = (0 until 4).map { idx ->
            PlayerInfo(id = "p$idx", name = "Player $idx", colorIndex = idx)
        }
        val state = DotsAndBoxesEngine.createInitialState(BoardSize.TINY, players).copy(
            players = listOf(
                players[0].copy(score = 2),
                players[1].copy(score = 2),
                players[2].copy(score = 0),
                players[3].copy(score = 0),
            ),
            phase = GamePhase.FINISHED,
        )
        assertEquals(2, state.winners.size)
        assertEquals(listOf("p0", "p1"), state.winners.map { it.id })
        assertEquals(listOf(2, 2, 0, 0), state.rankedPlayers.map { it.score })
    }

    @Test
    fun `discovery announcement encodes and decodes host metadata with source ip`() {
        val payload = NetworkProtocol.encodeDiscoveryAnnouncement(
            hostId = "host_xyz",
            hostName = "Raihan",
            port = 45678,
            boardSize = BoardSize.MEDIUM,
            playerCount = 2,
            maxPlayers = 4,
        )
        val decoded = NetworkProtocol.decodeDiscoveryAnnouncement(payload, "192.168.148.1")
        assertNotNull(decoded)
        assertEquals("host_xyz", decoded!!.hostId)
        assertEquals("Raihan", decoded.hostName)
        assertEquals("192.168.148.1", decoded.hostIp)
        assertEquals(45678, decoded.port)
        assertEquals(BoardSize.MEDIUM, decoded.boardSize)
        assertEquals(2, decoded.playerCount)
        assertEquals(4, decoded.maxPlayers)
    }

    @Test
    fun `midgame reconnect emits MatchStarted so client transitions from lobby back to game`() = runBlocking {
        val freePort = ServerSocket(0).use { it.localPort }
        val host = TcpHostServer()
        val client = TcpClientSession()
        try {
            assertTrue(
                host.startServer(
                    hostId = "host_1",
                    hostName = "HostPlayer",
                    initialBoardSize = BoardSize.TINY,
                    port = freePort,
                ).isSuccess,
            )
            client.connect("127.0.0.1", freePort, "client_1", "ClientPlayer")
            repeat(30) {
                if (client.snapshot.value.status == ClientConnectionStatus.CONNECTED_LOBBY) return@repeat
                delay(50)
            }
            assertTrue(host.startMatch().isSuccess)
            repeat(30) {
                if (client.snapshot.value.status == ClientConnectionStatus.IN_GAME) return@repeat
                delay(50)
            }

            // Simulate fresh client session reconnecting mid-game (after disconnect / app restart)
            client.close()
            val rejoiningClient = TcpClientSession()
            try {
                rejoiningClient.connect("127.0.0.1", freePort, "client_1", "ClientPlayer")
                var backInGame = false
                repeat(40) {
                    if (rejoiningClient.snapshot.value.status == ClientConnectionStatus.IN_GAME &&
                        rejoiningClient.snapshot.value.gameState != null
                    ) {
                        backInGame = true
                        return@repeat
                    }
                    delay(50)
                }
                assertTrue("Rejoining client must transition straight to IN_GAME", backInGame)
            } finally {
                rejoiningClient.close()
            }
        } finally {
            host.close()
        }
    }
}
