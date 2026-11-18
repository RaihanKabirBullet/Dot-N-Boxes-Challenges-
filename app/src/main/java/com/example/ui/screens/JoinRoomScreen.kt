package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.network.ClientConnectionStatus
import com.example.network.ClientSessionSnapshot
import com.example.network.DiscoveredRoom
import com.example.network.NetworkProtocol
import com.example.ui.components.GlassNavyCard
import com.example.ui.components.GradientActionButton
import com.example.ui.components.NavyGradientBackdrop
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.components.ScreenTopBar
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NavyCardElevated
import com.example.ui.theme.NavyOutline
import com.example.ui.theme.NavyVoid
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JoinRoomScreen(
    clientSnapshot: ClientSessionSnapshot,
    initialNickname: String,
    lastHostIp: String,
    detectedGatewayIp: String? = null,
    discoveredRooms: List<DiscoveredRoom> = emptyList(),
    onStartDiscovery: () -> Unit = {},
    onStopDiscovery: () -> Unit = {},
    onNicknameChange: (String) -> Unit,
    onConnect: (hostIp: String, portText: String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    DisposableEffect(Unit) {
        onStartDiscovery()
        onDispose {
            onStopDiscovery()
        }
    }

    var nickname by rememberSaveable { mutableStateOf(initialNickname) }
    var userEditedNickname by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(initialNickname) {
        if (!userEditedNickname && initialNickname != nickname) {
            nickname = initialNickname
        }
    }

    var hostIp by rememberSaveable { mutableStateOf(detectedGatewayIp ?: lastHostIp) }
    var userEditedIp by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(lastHostIp, detectedGatewayIp) {
        if (!userEditedIp) {
            val preferred = detectedGatewayIp?.takeIf { it.isNotBlank() } ?: lastHostIp
            if (preferred.isNotBlank()) {
                hostIp = preferred
            }
        }
    }

    var portText by rememberSaveable { mutableStateOf(NetworkProtocol.DEFAULT_PORT.toString()) }

    val isConnecting = clientSnapshot.status == ClientConnectionStatus.CONNECTING

    NavyGradientBackdrop {
        ResponsiveScreenContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
            ) {
                ScreenTopBar(
                    title = "Join Room",
                    subtitle = "Connect to a friend's Wi-Fi Hotspot",
                    onBack = onBack,
                    backTestTag = "join_room_back",
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Auto-discovered nearby rooms over Wi-Fi Hotspot / LAN
                    GlassNavyCard(
                        borderColor = if (discoveredRooms.isNotEmpty()) SuccessEmerald.copy(alpha = 0.55f) else AccentCyan.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Sensors,
                                        contentDescription = null,
                                        tint = if (discoveredRooms.isNotEmpty()) SuccessEmerald else AccentCyan,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (discoveredRooms.isNotEmpty()) "Nearby Rooms Found" else "Scanning Wi-Fi for Rooms...",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                    )
                                }
                                IconButton(
                                    onClick = onStartDiscovery,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("join_room_rescan_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Scan for nearby rooms",
                                        tint = AccentCyan,
                                    )
                                }
                            }

                            if (discoveredRooms.isEmpty()) {
                                Text(
                                    text = "Connect to the Host's Wi-Fi Hotspot (or same Wi-Fi network). Available rooms appear here automatically, or enter the Host IP below.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                )
                            } else {
                                discoveredRooms.forEach { room ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = NavyCardElevated,
                                        border = BorderStroke(1.dp, SuccessEmerald.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("discovered_room_${room.hostId}"),
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${room.hostName}'s Room",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = TextPrimary,
                                                )
                                                Text(
                                                    text = "${room.hostIp}:${room.port} • ${room.boardSize.dimensionsLabel} • ${room.playerCount}/${room.maxPlayers} players",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary,
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    userEditedIp = true
                                                    hostIp = room.hostIp
                                                    portText = room.port.toString()
                                                    onConnect(room.hostIp, room.port.toString())
                                                },
                                                enabled = !isConnecting,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = SuccessEmerald,
                                                    contentColor = NavyVoid,
                                                ),
                                                modifier = Modifier.testTag("discovered_room_join_${room.hostId}"),
                                            ) {
                                                Text("Join", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

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
                                    imageVector = Icons.Rounded.Wifi,
                                    contentDescription = null,
                                    tint = AccentCoral,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Manual Connection",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                )
                            }
                            Text(
                                text = "1. Connect this phone to the Host's Wi-Fi Hotspot.\n2. Tap the Host room above, or enter the Host IPv4 address shown on the Host screen.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                    }

                    GlassNavyCard(
                        borderColor = NavyOutline,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            OutlinedTextField(
                                value = nickname,
                                onValueChange = {
                                    if (it.length <= 18) {
                                        userEditedNickname = true
                                        nickname = it
                                        if (it.isNotBlank()) {
                                            onNicknameChange(it)
                                        }
                                    }
                                },
                                singleLine = true,
                                label = { Text("Your Nickname") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = NavyOutline,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedLabelColor = AccentCyan,
                                    unfocusedLabelColor = TextSecondary,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("join_room_nickname_input"),
                            )

                            OutlinedTextField(
                                value = hostIp,
                                onValueChange = {
                                    userEditedIp = true
                                    hostIp = it
                                },
                                singleLine = true,
                                label = { Text("Host IPv4 Address") },
                                placeholder = { Text("192.168.43.1") },
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentCoral,
                                    unfocusedBorderColor = NavyOutline,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedLabelColor = AccentCoral,
                                    unfocusedLabelColor = TextSecondary,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("join_room_ip_input"),
                            )

                            // Detected Wi-Fi gateway + Common Android Hotspot IP quick presets
                            val presets = buildList {
                                if (!detectedGatewayIp.isNullOrBlank()) {
                                    add(detectedGatewayIp to "Wi-Fi Host: $detectedGatewayIp")
                                }
                                listOf("192.168.43.1", "192.168.49.1", "192.168.1.1").forEach { ip ->
                                    if (ip != detectedGatewayIp) {
                                        add(ip to ip)
                                    }
                                }
                            }
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                presets.forEach { (ip, label) ->
                                    val isGateway = ip == detectedGatewayIp
                                    Surface(
                                        onClick = {
                                            userEditedIp = true
                                            hostIp = ip
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isGateway) AccentCyan.copy(alpha = 0.16f) else NavyCardElevated,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isGateway) AccentCyan else NavyOutline,
                                        ),
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AccentCyan,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = portText,
                                onValueChange = { portText = it.filter { ch -> ch.isDigit() }.take(5) },
                                singleLine = true,
                                label = { Text("TCP Port") },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = NavyOutline,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedLabelColor = AccentCyan,
                                    unfocusedLabelColor = TextSecondary,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("join_room_port_input"),
                            )
                        }
                    }

                    if (!clientSnapshot.errorMessage.isNullOrBlank()) {
                        GlassNavyCard(
                            borderColor = ErrorRose.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = "Connection Error",
                                    tint = ErrorRose,
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = clientSnapshot.errorMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ErrorRose,
                                )
                            }
                        }
                    }

                    if (isConnecting) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                color = AccentCoral,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Connecting to $hostIp...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    GradientActionButton(
                        text = if (isConnecting) "Connecting..." else "Join Room",
                        icon = Icons.Rounded.Lan,
                        enabled = !isConnecting && hostIp.isNotBlank(),
                        accentColors = listOf(AccentCoral, Color(0xFFFB7185)),
                        testTag = "join_room_connect_button",
                        onClick = { onConnect(hostIp, portText) },
                    )
                }
            }
        }
    }
}
