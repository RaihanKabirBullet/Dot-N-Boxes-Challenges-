package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.BoardSize
import com.example.network.HostRoomSnapshot
import com.example.ui.components.GlassNavyCard
import com.example.ui.components.GradientActionButton
import com.example.ui.components.NavyGradientBackdrop
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.components.ScreenTopBar
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NavyCardElevated
import com.example.ui.theme.NavyOutline
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun CreateRoomScreen(
    hostSnapshot: HostRoomSnapshot,
    initialNickname: String,
    onStartHosting: () -> Unit,
    onRefreshIp: () -> Unit,
    onNicknameChange: (String) -> Unit,
    onSelectBoardSize: (BoardSize) -> Unit,
    onEnterLobby: () -> Unit,
    onCopiedIp: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        if (!hostSnapshot.isRunning) {
            onStartHosting()
        } else {
            onRefreshIp()
        }
    }

    var nickname by remember(initialNickname) { mutableStateOf(initialNickname) }
    val clipboardManager = LocalClipboardManager.current

    NavyGradientBackdrop {
        ResponsiveScreenContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
            ) {
                ScreenTopBar(
                    title = "Create Room",
                    subtitle = "Host over Android Wi-Fi Hotspot",
                    onBack = onBack,
                    backTestTag = "create_room_back",
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Hotspot IP Display Card
                    GlassNavyCard(
                        borderColor = if (hostSnapshot.isRunning) AccentCyan.copy(alpha = 0.5f) else NavyOutline,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (hostSnapshot.isRunning) SuccessEmerald else WarningAmber),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (hostSnapshot.isRunning) "TCP SERVER LISTENING" else "STARTING SERVER...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (hostSnapshot.isRunning) SuccessEmerald else WarningAmber,
                                    )
                                }
                                IconButton(
                                    onClick = onRefreshIp,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("create_room_refresh_ip"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Refresh IP address",
                                        tint = AccentCyan,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Host IPv4 Address",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = hostSnapshot.hostIp ?: "Enable Hotspot & Tap Refresh",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = if (hostSnapshot.hostIp != null) AccentCyan else WarningAmber,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("create_room_host_ip"),
                                )
                                if (hostSnapshot.hostIp != null) {
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(hostSnapshot.hostIp))
                                            onCopiedIp()
                                        },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("create_room_copy_ip"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ContentCopy,
                                            contentDescription = "Copy Host IP",
                                            tint = TextPrimary,
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Port: ${hostSnapshot.port}  •  Max 4 Players",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )

                            if (hostSnapshot.alternateIps.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Other local IPs: ${hostSnapshot.alternateIps.joinToString(", ")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }

                    // Hotspot Instructions
                    GlassNavyCard(
                        borderColor = NavyOutline,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.WifiTethering,
                                    contentDescription = null,
                                    tint = AccentCyan,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Offline Hotspot Setup",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                )
                            }
                            Text(
                                text = "1. Turn on your Android Wi-Fi Hotspot in system settings.\n2. Have friends connect their devices to your hotspot Wi-Fi.\n3. Tap Refresh if you just enabled the hotspot, then share your Host IP.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                    }

                    // Host Nickname
                    GlassNavyCard(
                        borderColor = NavyOutline,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Host Nickname",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = nickname,
                                onValueChange = {
                                    if (it.length <= 18) {
                                        nickname = it
                                        onNicknameChange(it)
                                    }
                                },
                                singleLine = true,
                                placeholder = { Text("Enter your name") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = NavyOutline,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("create_room_nickname_input"),
                            )
                        }
                    }

                    // Initial Board Size Selector
                    GlassNavyCard(
                        borderColor = NavyOutline,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.GridView,
                                    contentDescription = null,
                                    tint = AccentCyan,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Board Size",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            val rows = BoardSize.entries.chunked(2)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                rows.forEach { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        pair.forEach { size ->
                                            val selected = hostSnapshot.boardSize == size
                                            Surface(
                                                onClick = { onSelectBoardSize(size) },
                                                shape = RoundedCornerShape(14.dp),
                                                color = if (selected) AccentCyan.copy(alpha = 0.16f) else NavyCardElevated,
                                                border = androidx.compose.foundation.BorderStroke(
                                                    width = 1.dp,
                                                    color = if (selected) AccentCyan else NavyOutline,
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("create_room_board_${size.name.lowercase()}"),
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = "${size.displayName} ${size.dots}×${size.dots}",
                                                            style = MaterialTheme.typography.labelLarge,
                                                            color = if (selected) AccentCyan else TextPrimary,
                                                        )
                                                        Text(
                                                            text = size.boxesLabel,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = TextSecondary,
                                                        )
                                                    }
                                                    if (selected) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.CheckCircle,
                                                            contentDescription = "Selected",
                                                            tint = AccentCyan,
                                                            modifier = Modifier.size(18.dp),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    GradientActionButton(
                        text = "Open Room Lobby (${hostSnapshot.players.size}/4)",
                        enabled = hostSnapshot.isRunning,
                        testTag = "create_room_enter_lobby_button",
                        onClick = onEnterLobby,
                    )
                }
            }
        }
    }
}
