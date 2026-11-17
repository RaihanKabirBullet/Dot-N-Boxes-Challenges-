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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.GameMode
import com.example.model.GameState
import com.example.ui.components.GlassNavyCard
import com.example.ui.components.GradientActionButton
import com.example.ui.components.NavyGradientBackdrop
import com.example.ui.components.PlayerColorBadge
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.components.SecondaryNavyButton
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentYellow
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NavyCardElevated
import com.example.ui.theme.NavyOutline
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ResultsScreen(
    mode: GameMode,
    gameState: GameState?,
    localPlayerId: String,
    onPlayAgain: () -> Unit,
    onBackToLobby: () -> Unit,
    onReturnHome: () -> Unit,
) {
    BackHandler(onBack = onReturnHome)

    val ranked = gameState?.rankedPlayers ?: emptyList()
    val winners = gameState?.winners ?: emptyList()
    val isTie = winners.size > 1
    val winnerColor = winners.firstOrNull()?.color?.primary ?: AccentYellow
    val totalBoxes = gameState?.boardSize?.totalBoxes ?: 1

    NavyGradientBackdrop {
        ResponsiveScreenContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            ) {
                // Hero Winner Card
                GlassNavyCard(
                    borderColor = winnerColor.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        winnerColor.copy(alpha = 0.22f),
                                        Color.Transparent,
                                    ),
                                ),
                            )
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = "Winner Trophy",
                            tint = winnerColor,
                            modifier = Modifier.size(56.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isTie) "IT'S A TIE!" else "VICTORY",
                            style = MaterialTheme.typography.labelSmall,
                            color = winnerColor,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when {
                                winners.isEmpty() -> "Match Finished"
                                isTie -> winners.joinToString(" & ") { it.name }
                                else -> "${winners.first().name} Wins!"
                            },
                            style = MaterialTheme.typography.displaySmall,
                            color = TextPrimary,
                            modifier = Modifier.testTag("results_winner_title"),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${gameState?.boardSize?.displayName ?: ""} ${gameState?.boardSize?.dimensionsLabel ?: ""} • $totalBoxes Boxes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "FINAL STANDINGS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ranked.forEachIndexed { idx, player ->
                        val isWinner = winners.any { it.id == player.id }
                        val isMe = player.id == localPlayerId
                        GlassNavyCard(
                            borderColor = if (isWinner) player.color.primary else NavyOutline,
                            backgroundColor = if (isWinner) NavyCardElevated else com.example.ui.theme.NavyCard,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("results_rank_card_${idx + 1}"),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "#${idx + 1}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = if (isWinner) player.color.primary else TextSecondary,
                                    modifier = Modifier.width(34.dp),
                                )
                                PlayerColorBadge(
                                    color = player.color,
                                    initial = player.initial,
                                    isActive = isWinner,
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
                                    }
                                    val pct = if (totalBoxes > 0) (player.score * 100) / totalBoxes else 0
                                    Text(
                                        text = "${player.color.label} • $pct% of board",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                    )
                                }
                                Text(
                                    text = "${player.score}",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = player.color.primary,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (mode == GameMode.PRACTICE_AI || mode == GameMode.HOST_MULTIPLAYER) {
                    GradientActionButton(
                        text = "Play Again",
                        icon = Icons.Rounded.Refresh,
                        testTag = "results_play_again_button",
                        onClick = onPlayAgain,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (mode != GameMode.PRACTICE_AI) {
                    SecondaryNavyButton(
                        text = "Return to Lobby",
                        accentColor = AccentCyan,
                        testTag = "results_lobby_button",
                        onClick = onBackToLobby,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                SecondaryNavyButton(
                    text = "Return Home",
                    icon = Icons.Rounded.Home,
                    accentColor = TextSecondary,
                    testTag = "results_home_button",
                    onClick = onReturnHome,
                )
            }
        }
    }
}
