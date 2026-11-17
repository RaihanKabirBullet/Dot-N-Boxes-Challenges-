package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.NavyGradientBackdrop
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.components.ScreenTopBar
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentYellow
import com.example.ui.theme.NavyCardElevated
import com.example.ui.theme.NavyVoid
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PlayMenuScreen(
    onWithAi: () -> Unit,
    onOnBoardMultiplayer: () -> Unit,
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
                    title = "Choose Play Mode",
                    subtitle = "100% Offline • No Internet Required",
                    onBack = onBack,
                    backTestTag = "play_menu_back",
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    PlayModeOptionCard(
                        title = "WITH AI",
                        badge = "SOLO PRACTICE",
                        description = "Challenge smart AI bots across any grid size from 3×3 to 10×10.",
                        icon = Icons.Rounded.SmartToy,
                        gradientColors = listOf(AccentYellow, Color(0xFFF59E0B)),
                        accentColor = AccentYellow,
                        testTag = "play_menu_with_ai",
                        onClick = onWithAi,
                    )

                    PlayModeOptionCard(
                        title = "ON BOARD MULTIPLAYER",
                        badge = "2–10 PLAYERS • SAME PHONE",
                        description = "Pass-and-play locally on the same device. No Wi-Fi, hotspot, or network needed.",
                        icon = Icons.Rounded.Groups,
                        gradientColors = listOf(AccentCyan, AccentPurple),
                        accentColor = AccentCyan,
                        testTag = "play_menu_on_board",
                        onClick = onOnBoardMultiplayer,
                    )

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = NavyCardElevated.copy(alpha = 0.7f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentCoral.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Tip: Want to play across separate phones? Go back to the Home Screen and use Offline Multiplayer (Host / Join).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayModeOptionCard(
    title: String,
    badge: String,
    description: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(NavyCardElevated)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        gradientColors.first().copy(alpha = 0.22f),
                        gradientColors.last().copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                ),
            )
            .border(
                width = 1.5.dp,
                color = accentColor.copy(alpha = 0.55f),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(gradientColors)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = NavyVoid,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.16f),
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
