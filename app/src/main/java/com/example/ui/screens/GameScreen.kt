package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.GameMode
import com.example.model.GamePhase
import com.example.model.GameState
import com.example.model.LineId
import com.example.model.PlayerInfo
import com.example.network.ClientConnectionStatus
import com.example.ui.components.DotsAndBoxesBoard
import com.example.ui.components.GlassNavyCard
import com.example.ui.components.NavyGradientBackdrop
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentYellow
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardElevated
import com.example.ui.theme.NavyOutline
import com.example.ui.theme.NavyOutlineSubtle
import com.example.ui.theme.NavyVoid
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GameScreen(
    mode: GameMode,
    gameState: GameState?,
    localPlayerId: String,
    soundEnabled: Boolean,
    clientStatus: ClientConnectionStatus,
    onToggleSound: () -> Unit,
    onPause: () -> Unit,
    onDotSelectedFeedback: () -> Unit,
    onInvalidTapFeedback: () -> Unit,
    onLineCommitted: (LineId) -> Unit,
    onReconnectClient: () -> Unit,
) {
    BackHandler(onBack = onPause)

    NavyGradientBackdrop {
        ResponsiveScreenContainer(maxWidth = 720.dp) {
            if (gameState == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Loading match...",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary,
                    )
                }
                return@ResponsiveScreenContainer
            }

            val currentPlayer = gameState.currentPlayer
            val isMyTurn = currentPlayer?.id == localPlayerId && gameState.phase == GamePhase.IN_PROGRESS
            val isClientDisconnected = mode == GameMode.CLIENT_MULTIPLAYER &&
                (clientStatus == ClientConnectionStatus.DISCONNECTED || clientStatus == ClientConnectionStatus.ERROR)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                // Top Header & Control Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = NavyCard,
                        border = BorderStroke(1.dp, NavyOutlineSubtle),
                    ) {
                        IconButton(
                            onClick = onPause,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("game_pause_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Pause,
                                contentDescription = "Pause Match",
                                tint = TextPrimary,
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                    ) {
                        Text(
                            text = "Dot N Box Challenge",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("game_header_title"),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = NavyCard,
                            border = BorderStroke(1.dp, NavyOutline),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "${gameState.boardSize.dots}×${gameState.boardSize.dots}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentCyan,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "•",
                                    color = TextSecondary,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${gameState.remainingBoxesCount} boxes left",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary,
                                    modifier = Modifier.testTag("game_remaining_boxes"),
                                )
                            }
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = NavyCard,
                        border = BorderStroke(1.dp, NavyOutlineSubtle),
                    ) {
                        IconButton(
                            onClick = onToggleSound,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("game_sound_toggle"),
                        ) {
                            Icon(
                                imageVector = if (soundEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                                contentDescription = if (soundEnabled) "Mute Sound" else "Enable Sound",
                                tint = if (soundEnabled) AccentCyan else TextSecondary,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scoreboard Strip (2-4 players)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    gameState.players.forEachIndexed { index, player ->
                        val isTurn = index == gameState.currentTurnIndex && gameState.phase == GamePhase.IN_PROGRESS
                        val isMe = player.id == localPlayerId
                        PlayerScorePill(
                            player = player,
                            isActiveTurn = isTurn,
                            isMe = isMe,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Client Disconnect Banner (with 1-tap reconnect)
                AnimatedVisibility(visible = isClientDisconnected) {
                    GlassNavyCard(
                        borderColor = ErrorRose,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WifiOff,
                                    contentDescription = "Disconnected",
                                    tint = ErrorRose,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Connection lost",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ErrorRose,
                                )
                            }
                            Button(
                                onClick = onReconnectClient,
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRose, contentColor = TextPrimary),
                                modifier = Modifier.testTag("game_reconnect_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reconnect", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                // Main Canvas Board
                DotsAndBoxesBoard(
                    gameState = gameState,
                    localPlayerId = localPlayerId,
                    isMyTurn = isMyTurn && !isClientDisconnected,
                    onDotSelectedFeedback = onDotSelectedFeedback,
                    onInvalidTapFeedback = onInvalidTapFeedback,
                    onLineCommitted = onLineCommitted,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Turn & Bonus Status Banner
                val activeColor = currentPlayer?.color?.primary ?: AccentCyan
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (gameState.bonusTurnAwarded) {
                        AccentYellow.copy(alpha = 0.14f)
                    } else {
                        NavyCard
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (gameState.bonusTurnAwarded) AccentYellow else activeColor.copy(alpha = 0.45f),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("game_turn_banner"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        AnimatedVisibility(
                            visible = gameState.bonusTurnAwarded,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = "Bonus Turn",
                                    tint = AccentYellow,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                        }

                        val bannerText = when {
                            gameState.phase == GamePhase.PAUSED -> "Match Paused"
                            gameState.phase == GamePhase.FINISHED -> "All boxes claimed! Calculating results..."
                            gameState.bonusTurnAwarded && isMyTurn -> "BONUS TURN! Tap two adjacent dots"
                            gameState.bonusTurnAwarded -> "${currentPlayer?.name ?: "Player"} earned a BONUS TURN!"
                            isMyTurn -> "Your Turn — Tap two adjacent dots"
                            currentPlayer?.isAi == true -> "${currentPlayer.name} is thinking..."
                            else -> "${currentPlayer?.name ?: "Player"}'s Turn"
                        }

                        Text(
                            text = bannerText,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (gameState.bonusTurnAwarded) AccentYellow else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerScorePill(
    player: PlayerInfo,
    isActiveTurn: Boolean,
    isMe: Boolean,
    modifier: Modifier = Modifier,
) {
    val pColor = player.color.primary
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isActiveTurn) pColor.copy(alpha = 0.16f) else NavyCardElevated,
        border = BorderStroke(
            width = if (isActiveTurn) 2.dp else 1.dp,
            color = if (isActiveTurn) pColor else NavyOutlineSubtle,
        ),
        modifier = modifier.testTag("score_pill_${player.id}"),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (player.isConnected) pColor else ErrorRose,
                        ),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (isMe) "${player.name}*" else player.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isActiveTurn) TextPrimary else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = player.score.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                ),
                color = pColor,
            )
            if (isActiveTurn) {
                Text(
                    text = "TURN",
                    style = MaterialTheme.typography.labelSmall,
                    color = SuccessEmerald,
                )
            }
        }
    }
}
