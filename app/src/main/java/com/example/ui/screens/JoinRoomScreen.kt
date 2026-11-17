package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun JoinRoomScreen(
    clientSnapshot: ClientSessionSnapshot,
    initialNickname: String,
    lastHostIp: String,
    onNicknameChange: (String) -> Unit,
    onConnect: (hostIp: String, portText: String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var nickname by remember(initialNickname) { mutableStateOf(initialNickname) }
    var hostIp by remember(lastHostIp) { mutableStateOf(lastHostIp) }
    var portText by remember { mutableStateOf(NetworkProtocol.DEFAULT_PORT.toString()) }

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
                                    text = "Before You Connect",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                )
                            }
                            Text(
                                text = "1. Connect this device to the Host's Wi-Fi Hotspot.\n2. Enter the Host IPv4 address shown on the Host's Create Room or Lobby screen.",
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
                                        nickname = it
                                        onNicknameChange(it)
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
                                onValueChange = { hostIp = it },
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

                            // Common Android Hotspot IP quick presets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf("192.168.43.1", "192.168.49.1", "192.168.1.1").forEach { preset ->
                                    Surface(
                                        onClick = { hostIp = preset },
                                        shape = RoundedCornerShape(10.dp),
                                        color = NavyCardElevated,
                                        border = BorderStroke(1.dp, NavyOutline),
                                    ) {
                                        Text(
                                            text = preset,
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
