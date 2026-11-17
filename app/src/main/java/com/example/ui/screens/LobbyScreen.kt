package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.BoardSize
import com.example.model.GameMode
import com.example.model.PlayerInfo
import com.example.network.ClientSessionSnapshot
import com.example.network.HostRoomSnapshot
import com.example.ui.components.GlassNavyCard
import com.example.ui.components.GradientActionButton
import com.example.ui.components.NavyGradientBackdrop
import com.example.ui.components.PlayerColorBadge
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.components.ScreenTopBar
import com.example.ui.components.SecondaryNavyButton
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NavyCardElevated
import com.example.ui.theme.NavyOutline
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LobbyScreen(
    mode: GameMode,
    localPlayerId: String,
    hostSnapshot: HostRoomSnapshot,
    clientSnapshot: ClientSessionSnapshot,
    onRefreshHostIp: () -> Unit,
    onOpenBoardSelection: () -> Unit,
    onKickPlayer: (String) -> Unit,
    onStartMatch: () -> Unit,
    onCopiedIp: () -> Unit,
    onLeaveLobby: () -> Unit,
) {
    BackHandler(onBack = onLeaveLobby)

    val isHost = mode == GameMode.HOST_MULTIPLAYER
    val players: List<PlayerInfo> = if (isHost) hostSnapshot.players else clientSnapshot.players
    val boardSize: BoardSize = if (isHost) hostSnapshot.boardSize else clientSnapshot.boardSize
    val roomIp: String? = if (isHost) hostSnapshot.hostIp else clientSnapshot.hostIp
    val roomPort: Int = if (isHost) hostSnapshot.port else clientSnapshot.port
    val connectedCount = players.count { it.isConnected }
    val clipboardManager = LocalClipboardManager.current

    NavyGradientBackdrop {
        ResponsiveScreenContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
            ) {
                ScreenTopBar(
                    title = "Room Lobby",
                    subtitle = if (isHost) "You are the Host" else "Connected to Host",
                    onBack = onLeaveLobby,
                    backTestTag = "lobby_leave_back",
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Connection / Room Info Card
                    item {
                        GlassNavyCard(
                            borderColor = AccentCyan.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isHost) "SHARE HOST IP" else "CONNECTED HOST",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AccentCyan,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${roomIp ?: "Unavailable"} : $roomPort",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = TextPrimary,
                                        modifier = Modifier.testTag("lobby_ip_display"),
                                    )
                                }
                                if (isHost) {
                                    Row {
                                        IconButton(
                                            onClick = onRefreshHostIp,
                                            modifier = Modifier.minimumInteractiveComponentSize(),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Refresh,
                                                contentDescription = "Refresh IP",
                                                tint = AccentCyan,
                                            )
                                        }
                                        if (roomIp != null) {
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(roomIp))
                                                    onCopiedIp()
                                                },
                                                modifier = Modifier.minimumInteractiveComponentSize(),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.ContentCopy,
                                                    contentDescription = "Copy IP",
                                                    tint = TextPrimary,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Board Size Sync Card
                    item {
                        GlassNavyCard(
                            borderColor = NavyOutline,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.GridView,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(22.dp),
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "BOARD SIZE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                        )
                                        Text(
                                            text = "${boardSize.displayName} (${boardSize.dimensionsLabel} • ${boardSize.boxesLabel})",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                            modifier = Modifier.testTag("lobby_board_size_text"),
                                        )
                                    }
                                }
                                if (isHost) {
                                    TextButton(
                                        onClick = onOpenBoardSelection,
                                        modifier = Modifier.testTag("lobby_change_board_button"),
                                    ) {
                                        Text(
                                            text = "Change",
                                            color = AccentCyan,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Players Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "PLAYERS ($connectedCount / 4)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                            Text(
                                text = if (connectedCount >= 2) "Ready to start" else "Need at least 2 players",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (connectedCount >= 2) SuccessEmerald else AccentCoral,
                            )
                        }
                    }

                    // Player Cards
                    items(players, key = { it.id }) { player ->
                        val isMe = player.id == localPlayerId
                        GlassNavyCard(
                            borderColor = player.color.primary.copy(alpha = 0.45f),
                            backgroundColor = NavyCardElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lobby_player_card_${player.id}"),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PlayerColorBadge(
                                    color = player.color,
                                    initial = player.initial,
                                    isActive = true,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = player.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                        )
                                        if (isMe) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = player.color.primary.copy(alpha = 0.2f),
                                            ) {
                                                Text(
                                                    text = "YOU",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = player.color.primary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                        if (player.isHost) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = AccentCyan.copy(alpha = 0.16f),
                                            ) {
                                                Text(
                                                    text = "HOST",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = AccentCyan,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(if (player.isConnected) SuccessEmerald else ErrorRose),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${player.color.label} • ${if (player.isConnected) "Connected" else "Disconnected"}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary,
                                        )
                                    }
                                }
                                if (isHost && !player.isHost) {
                                    IconButton(
                                        onClick = { onKickPlayer(player.id) },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("lobby_kick_${player.id}"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Remove ${player.name}",
                                            tint = ErrorRose,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Empty slots hint
                    if (players.size < 4) {
                        items(4 - players.size) { idx ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = NavyCardElevated.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, NavyOutline.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "Open Player Slot #${players.size + idx + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary.copy(alpha = 0.65f),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }

                // Footer Actions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (isHost) {
                        GradientActionButton(
                            text = if (connectedCount >= 2) "Start Match ($connectedCount Players)" else "Waiting for Players (Min 2)",
                            icon = Icons.Rounded.PlayArrow,
                            enabled = connectedCount >= 2,
                            testTag = "lobby_start_match_button",
                            onClick = onStartMatch,
                        )
                    } else {
                        GlassNavyCard(
                            borderColor = AccentCyan.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "Waiting for Host to start the match...",
                                style = MaterialTheme.typography.titleMedium,
                                color = AccentCyan,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                            )
                        }
                        SecondaryNavyButton(
                            text = "Leave Room",
                            accentColor = ErrorRose,
                            testTag = "lobby_client_leave_button",
                            onClick = onLeaveLobby,
                        )
                    }
                }
            }
        }
    }
}
