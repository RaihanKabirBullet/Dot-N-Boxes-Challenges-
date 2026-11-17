package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.BoardSize
import com.example.model.PlayerColor
import com.example.ui.components.GlassNavyCard
import com.example.ui.components.GradientActionButton
import com.example.ui.components.NavyGradientBackdrop
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.components.ScreenTopBar
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NavyCardElevated
import com.example.ui.theme.NavyOutline
import com.example.ui.theme.NavyOutlineSubtle
import com.example.ui.theme.NavyVoid
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun OnBoardSetupScreen(
    playerCount: Int,
    selectedBoardSize: BoardSize,
    playerNames: List<String>,
    defaultNameProvider: (Int) -> String,
    onPlayerCountChange: (Int) -> Unit,
    onBoardSizeChange: (BoardSize) -> Unit,
    onPlayerNameChange: (Int, String) -> Unit,
    onStartGame: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    NavyGradientBackdrop {
        ResponsiveScreenContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
            ) {
                ScreenTopBar(
                    title = "On Board Multiplayer",
                    subtitle = "Pass & Play • 2–10 Players",
                    onBack = onBack,
                    backTestTag = "on_board_setup_back",
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Player Count Selector (2–10, default 2)
                    item {
                        GlassNavyCard(
                            borderColor = AccentCyan.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Groups,
                                            contentDescription = null,
                                            tint = AccentCyan,
                                            modifier = Modifier.size(22.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Number of Players",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TextPrimary,
                                            )
                                            Text(
                                                text = "Select 2 to 10 players",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextSecondary,
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = NavyCardElevated,
                                            border = BorderStroke(1.dp, NavyOutline),
                                        ) {
                                            IconButton(
                                                onClick = { onPlayerCountChange(playerCount - 1) },
                                                enabled = playerCount > 2,
                                                modifier = Modifier
                                                    .minimumInteractiveComponentSize()
                                                    .testTag("on_board_count_minus"),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Remove,
                                                    contentDescription = "Decrease players",
                                                    tint = if (playerCount > 2) TextPrimary else TextSecondary.copy(alpha = 0.4f),
                                                )
                                            }
                                        }
                                        Text(
                                            text = "$playerCount",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = AccentCyan,
                                            modifier = Modifier
                                                .padding(horizontal = 14.dp)
                                                .testTag("on_board_player_count_value"),
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = NavyCardElevated,
                                            border = BorderStroke(1.dp, NavyOutline),
                                        ) {
                                            IconButton(
                                                onClick = { onPlayerCountChange(playerCount + 1) },
                                                enabled = playerCount < 10,
                                                modifier = Modifier
                                                    .minimumInteractiveComponentSize()
                                                    .testTag("on_board_count_plus"),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Add,
                                                    contentDescription = "Increase players",
                                                    tint = if (playerCount < 10) TextPrimary else TextSecondary.copy(alpha = 0.4f),
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items((2..10).toList()) { count ->
                                        val selected = playerCount == count
                                        Surface(
                                            onClick = { onPlayerCountChange(count) },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (selected) AccentCyan else NavyCardElevated,
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = if (selected) AccentCyan else NavyOutlineSubtle,
                                            ),
                                            modifier = Modifier.testTag("on_board_count_chip_$count"),
                                        ) {
                                            Text(
                                                text = "$count",
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                                color = if (selected) NavyVoid else TextPrimary,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Board Size Selector
                    item {
                        GlassNavyCard(
                            borderColor = NavyOutline,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.GridView,
                                        contentDescription = null,
                                        tint = AccentPurple,
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
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(BoardSize.entries) { size ->
                                        val selected = selectedBoardSize == size
                                        Surface(
                                            onClick = { onBoardSizeChange(size) },
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (selected) AccentPurple.copy(alpha = 0.2f) else NavyCardElevated,
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = if (selected) AccentPurple else NavyOutlineSubtle,
                                            ),
                                            modifier = Modifier.testTag("on_board_size_${size.name.lowercase()}"),
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                            ) {
                                                Text(
                                                    text = "${size.displayName} ${size.dots}×${size.dots}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = if (selected) AccentPurple else TextPrimary,
                                                )
                                                Text(
                                                    text = size.boxesLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Player Names Section Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "PLAYER NAMES (OPTIONAL)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                            Text(
                                text = "Blank = default name",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentCyan,
                            )
                        }
                    }

                    // Dynamic Player Name Fields (2..10)
                    items(playerCount, key = { it }) { idx ->
                        val pColor = PlayerColor.fromIndex(idx)
                        val defaultName = defaultNameProvider(idx)
                        val currentText = playerNames.getOrElse(idx) { "" }

                        GlassNavyCard(
                            borderColor = pColor.primary.copy(alpha = 0.45f),
                            backgroundColor = NavyCardElevated,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(pColor.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = pColor.primary,
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                OutlinedTextField(
                                    value = currentText,
                                    onValueChange = { onPlayerNameChange(idx, it) },
                                    singleLine = true,
                                    label = { Text("Player ${idx + 1} (${pColor.label})") },
                                    placeholder = { Text(defaultName) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = pColor.primary,
                                        unfocusedBorderColor = NavyOutline,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedLabelColor = pColor.primary,
                                        unfocusedLabelColor = TextSecondary,
                                        focusedPlaceholderColor = TextSecondary.copy(alpha = 0.6f),
                                        unfocusedPlaceholderColor = TextSecondary.copy(alpha = 0.6f),
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("on_board_player_name_${idx + 1}"),
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(6.dp)) }
                }

                // Functional START GAME button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    GradientActionButton(
                        text = "START GAME ($playerCount Players)",
                        icon = Icons.Rounded.PlayArrow,
                        accentColors = listOf(AccentCyan, Color(0xFF0EA5E9)),
                        testTag = "on_board_start_game_button",
                        onClick = onStartGame,
                    )
                }
            }
        }
    }
}
