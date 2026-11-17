package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.UserSettings
import com.example.ui.components.BrandDotsLogoMini
import com.example.ui.components.GlassNavyCard
import com.example.ui.components.NavyGradientBackdrop
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentYellow
import com.example.ui.theme.BoardDotColor
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardElevated
import com.example.ui.theme.NavyOutline
import com.example.ui.theme.NavyOutlineSubtle
import com.example.ui.theme.NavyVoid
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    settings: UserSettings,
    onUpdateNickname: (String) -> Unit,
    onToggleSound: () -> Unit,
    onToggleVibration: () -> Unit,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit,
    onPracticeAi: () -> Unit,
    onHowToPlay: () -> Unit,
) {
    var showSettingsDialog by remember { mutableStateOf(false) }

    NavyGradientBackdrop {
        ResponsiveScreenContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                // 1. Top Branding: DOT N BOX CHALLENGE
                GlassNavyCard(
                    borderColor = AccentCyan.copy(alpha = 0.35f),
                    backgroundColor = NavyCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_branding_card"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        AccentCyan.copy(alpha = 0.14f),
                                        AccentPurple.copy(alpha = 0.10f),
                                        Color.Transparent,
                                    ),
                                ),
                            )
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BrandDotsLogoMini()
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "OFFLINE STRATEGY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentCyan,
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "DOT N BOX CHALLENGE",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                onClick = { showSettingsDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                color = NavyCardElevated,
                                border = BorderStroke(1.dp, NavyOutlineSubtle),
                                modifier = Modifier.testTag("home_player_chip"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = "Player Nickname",
                                        tint = AccentCyan,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = settings.nickname,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        MiniBoardArtPreview(modifier = Modifier.size(86.dp))
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // 2. Large, prominent PLAY card/button -> opens Practice vs AI
                val playShape = RoundedCornerShape(24.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(playShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AccentCyan, Color(0xFF0EA5E9), Color(0xFF2563EB)),
                            ),
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.28f),
                            shape = playShape,
                        )
                        .clickable(onClick = onPracticeAi)
                        .testTag("home_play_button"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 22.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(NavyVoid.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Play",
                                    tint = NavyVoid,
                                    modifier = Modifier.size(34.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "PLAY",
                                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                    color = NavyVoid,
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.SmartToy,
                                        contentDescription = null,
                                        tint = NavyVoid.copy(alpha = 0.85f),
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Practice vs AI • Offline",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = NavyVoid.copy(alpha = 0.88f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = NavyVoid,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                // 3. Section Heading: OFFLINE MULTIPLAYER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "OFFLINE MULTIPLAYER",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                    )
                    Text(
                        text = "Wi-Fi Hotspot LAN",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Side-by-side HOST and JOIN buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MultiplayerActionTile(
                        title = "HOST",
                        subtitle = "Create Room",
                        icon = Icons.Rounded.WifiTethering,
                        accentColor = AccentCyan,
                        testTag = "home_host_button",
                        modifier = Modifier.weight(1f),
                        onClick = onCreateRoom,
                    )
                    MultiplayerActionTile(
                        title = "JOIN",
                        subtitle = "Enter Host IP",
                        icon = Icons.Rounded.Lan,
                        accentColor = AccentCoral,
                        testTag = "home_join_button",
                        modifier = Modifier.weight(1f),
                        onClick = onJoinRoom,
                    )
                }

                Spacer(modifier = Modifier.weight(1f, fill = true))
                Spacer(modifier = Modifier.height(26.dp))

                // 5. Bottom: How to Play and Settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BottomUtilityCard(
                        label = "How to Play",
                        subtitle = "Rules & Hotspot",
                        icon = Icons.AutoMirrored.Rounded.HelpOutline,
                        accentColor = AccentPurple,
                        testTag = "home_how_to_play_button",
                        modifier = Modifier.weight(1f),
                        onClick = onHowToPlay,
                    )
                    BottomUtilityCard(
                        label = "Settings",
                        subtitle = if (settings.soundEnabled) "Sound On" else "Muted",
                        icon = Icons.Rounded.Settings,
                        accentColor = AccentYellow,
                        testTag = "home_settings_button",
                        modifier = Modifier.weight(1f),
                        onClick = { showSettingsDialog = true },
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showSettingsDialog) {
        HomeSettingsDialog(
            settings = settings,
            onDismiss = { showSettingsDialog = false },
            onSaveNickname = onUpdateNickname,
            onToggleSound = onToggleSound,
            onToggleVibration = onToggleVibration,
        )
    }
}

@Composable
private fun MultiplayerActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    GlassNavyCard(
        onClick = onClick,
        borderColor = accentColor.copy(alpha = 0.5f),
        backgroundColor = NavyCardElevated,
        modifier = modifier.testTag(testTag),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.14f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BottomUtilityCard(
    label: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    GlassNavyCard(
        onClick = onClick,
        borderColor = NavyOutline,
        backgroundColor = NavyCard,
        modifier = modifier.testTag(testTag),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MiniBoardArtPreview(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val pad = size.width * 0.14f
        val step = (size.width - pad * 2f) / 2f
        fun pt(r: Int, c: Int) = Offset(pad + c * step, pad + r * step)

        drawRoundRect(
            color = AccentCyan.copy(alpha = 0.28f),
            topLeft = Offset(pad + 4f, pad + 4f),
            size = Size(step - 8f, step - 8f),
            cornerRadius = CornerRadius(10f, 10f),
        )
        drawRoundRect(
            color = AccentCoral.copy(alpha = 0.28f),
            topLeft = Offset(pad + step + 4f, pad + step + 4f),
            size = Size(step - 8f, step - 8f),
            cornerRadius = CornerRadius(10f, 10f),
        )

        val stroke = 6f
        drawLine(AccentCyan, pt(0, 0), pt(0, 1), stroke, StrokeCap.Round)
        drawLine(AccentCyan, pt(0, 1), pt(1, 1), stroke, StrokeCap.Round)
        drawLine(AccentCyan, pt(1, 0), pt(1, 1), stroke, StrokeCap.Round)
        drawLine(AccentCyan, pt(0, 0), pt(1, 0), stroke, StrokeCap.Round)

        drawLine(AccentCoral, pt(1, 1), pt(1, 2), stroke, StrokeCap.Round)
        drawLine(AccentCoral, pt(1, 2), pt(2, 2), stroke, StrokeCap.Round)
        drawLine(AccentCoral, pt(2, 1), pt(2, 2), stroke, StrokeCap.Round)
        drawLine(AccentCoral, pt(1, 1), pt(2, 1), stroke, StrokeCap.Round)

        drawLine(AccentYellow, pt(0, 1), pt(0, 2), stroke, StrokeCap.Round)
        drawLine(AccentPurple, pt(1, 0), pt(2, 0), stroke, StrokeCap.Round)

        for (r in 0..2) {
            for (c in 0..2) {
                drawCircle(color = BoardDotColor, radius = 6.5f, center = pt(r, c))
            }
        }
    }
}

@Composable
private fun HomeSettingsDialog(
    settings: UserSettings,
    onDismiss: () -> Unit,
    onSaveNickname: (String) -> Unit,
    onToggleSound: () -> Unit,
    onToggleVibration: () -> Unit,
) {
    var nicknameText by remember(settings.nickname) { mutableStateOf(settings.nickname) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = nicknameText,
                    onValueChange = { if (it.length <= 18) nicknameText = it },
                    singleLine = true,
                    label = { Text("Player Nickname") },
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
                        .testTag("settings_nickname_input"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (settings.soundEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sound Effects",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                        )
                    }
                    Switch(
                        checked = settings.soundEnabled,
                        onCheckedChange = { onToggleSound() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NavyVoid,
                            checkedTrackColor = AccentCyan,
                        ),
                        modifier = Modifier.testTag("settings_sound_switch"),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Vibration,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Haptic Vibration",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                        )
                    }
                    Switch(
                        checked = settings.vibrationEnabled,
                        onCheckedChange = { onToggleVibration() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NavyVoid,
                            checkedTrackColor = AccentPurple,
                        ),
                        modifier = Modifier.testTag("settings_vibration_switch"),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveNickname(nicknameText)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = NavyVoid),
                modifier = Modifier.testTag("settings_save_button"),
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        },
    )
}
