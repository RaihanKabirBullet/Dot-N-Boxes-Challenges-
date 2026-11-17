package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.UserSettings
import com.example.model.GameMode
import com.example.ui.components.GlassNavyCard
import com.example.ui.components.GradientActionButton
import com.example.ui.components.NavyGradientBackdrop
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.components.SecondaryNavyButton
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.NavyOutline
import com.example.ui.theme.NavyVoid
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PauseScreen(
    mode: GameMode,
    settings: UserSettings,
    onToggleSound: () -> Unit,
    onToggleVibration: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onHowToPlay: () -> Unit,
    onReturnHome: () -> Unit,
) {
    BackHandler(onBack = onResume)

    NavyGradientBackdrop {
        ResponsiveScreenContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "MATCH PAUSED",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentCyan,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Take a Breather",
                    style = MaterialTheme.typography.displaySmall,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(20.dp))

                GlassNavyCard(
                    borderColor = NavyOutline,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    text = "Sound Effects",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "Line & box audio cues",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                )
                            }
                            Switch(
                                checked = settings.soundEnabled,
                                onCheckedChange = { onToggleSound() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NavyVoid,
                                    checkedTrackColor = AccentCyan,
                                ),
                                modifier = Modifier.testTag("pause_sound_switch"),
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    text = "Haptic Vibration",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "Tactile feedback on taps",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                )
                            }
                            Switch(
                                checked = settings.vibrationEnabled,
                                onCheckedChange = { onToggleVibration() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NavyVoid,
                                    checkedTrackColor = AccentPurple,
                                ),
                                modifier = Modifier.testTag("pause_vibration_switch"),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                GradientActionButton(
                    text = "Resume Match",
                    icon = Icons.Rounded.PlayArrow,
                    testTag = "pause_resume_button",
                    onClick = onResume,
                )

                if (mode != GameMode.CLIENT_MULTIPLAYER) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SecondaryNavyButton(
                        text = "Restart Match",
                        icon = Icons.Rounded.Refresh,
                        accentColor = AccentCoral,
                        testTag = "pause_restart_button",
                        onClick = onRestart,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                SecondaryNavyButton(
                    text = "How to Play",
                    icon = Icons.AutoMirrored.Rounded.HelpOutline,
                    accentColor = AccentPurple,
                    testTag = "pause_how_to_play_button",
                    onClick = onHowToPlay,
                )

                Spacer(modifier = Modifier.height(12.dp))

                SecondaryNavyButton(
                    text = "Leave to Home",
                    icon = Icons.Rounded.Home,
                    accentColor = ErrorRose,
                    testTag = "pause_home_button",
                    onClick = onReturnHome,
                )
            }
        }
    }
}
