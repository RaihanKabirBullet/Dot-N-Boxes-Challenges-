package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.BoardSize
import com.example.ui.components.GlassNavyCard
import com.example.ui.components.GradientActionButton
import com.example.ui.components.NavyGradientBackdrop
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.components.ScreenTopBar
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentYellow
import com.example.ui.theme.BoardDotColor
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardElevated
import com.example.ui.theme.NavyOutline
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun BoardSelectionScreen(
    isPracticeMode: Boolean,
    selectedBoardSize: BoardSize,
    aiCount: Int,
    onSelectBoardSize: (BoardSize) -> Unit,
    onSelectAiCount: (Int) -> Unit,
    onConfirm: () -> Unit,
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
                    title = "Select Board",
                    subtitle = if (isPracticeMode) "Practice vs AI Setup" else "Choose Grid Size for Lobby",
                    onBack = onBack,
                    backTestTag = "board_selection_back",
                )

                if (isPracticeMode) {
                    GlassNavyCard(
                        borderColor = AccentYellow.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.SmartToy,
                                    contentDescription = null,
                                    tint = AccentYellow,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI Opponents",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                listOf(1, 2, 3).forEach { count ->
                                    val active = aiCount == count
                                    Surface(
                                        onClick = { onSelectAiCount(count) },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (active) AccentYellow.copy(alpha = 0.18f) else NavyCardElevated,
                                        border = BorderStroke(1.dp, if (active) AccentYellow else NavyOutline),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("board_ai_count_$count"),
                                    ) {
                                        Text(
                                            text = "${count + 1}P (${count} AI)",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (active) AccentYellow else TextPrimary,
                                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(BoardSize.entries) { size ->
                        val isSelected = selectedBoardSize == size
                        GlassNavyCard(
                            onClick = { onSelectBoardSize(size) },
                            borderColor = if (isSelected) AccentCyan else NavyOutline,
                            backgroundColor = if (isSelected) NavyCardElevated else NavyCard,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("board_size_card_${size.name.lowercase()}"),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = size.displayName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) AccentCyan else TextPrimary,
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = AccentCyan,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                MiniDotGridPreview(
                                    dots = size.dots,
                                    accentColor = if (isSelected) AccentCyan else BoardDotColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(68.dp),
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = size.dimensionsLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "${size.boxesLabel} • ${size.tagline}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    GradientActionButton(
                        text = if (isPracticeMode) "Start Practice Match" else "Apply Board Size",
                        icon = Icons.Rounded.PlayArrow,
                        accentColors = if (isPracticeMode) {
                            listOf(AccentYellow, Color(0xFFF59E0B))
                        } else {
                            listOf(AccentCyan, Color(0xFF0EA5E9))
                        },
                        testTag = "board_selection_confirm_button",
                        onClick = onConfirm,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniDotGridPreview(
    dots: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val pad = size.width * 0.12f
        val span = size.width - pad * 2f
        val step = span / (dots - 1).coerceAtLeast(1)
        val dotRadius = when {
            dots <= 4 -> 4.5f
            dots <= 6 -> 3.4f
            dots <= 8 -> 2.5f
            else -> 2.0f
        }
        for (r in 0 until dots) {
            for (c in 0 until dots) {
                drawCircle(
                    color = accentColor,
                    radius = dotRadius,
                    center = Offset(pad + c * step, pad + r * step),
                )
            }
        }
    }
}
