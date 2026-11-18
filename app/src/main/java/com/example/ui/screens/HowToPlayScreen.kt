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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.GlassNavyCard
import com.example.ui.components.NavyGradientBackdrop
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.components.ScreenTopBar
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentYellow
import com.example.ui.theme.NavyOutline
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HowToPlayScreen(
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    NavyGradientBackdrop {
        ResponsiveScreenContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .testTag("how_to_play_screen"),
            ) {
                ScreenTopBar(
                    title = "How to Play",
                    subtitle = "Rules & Offline Hotspot Guide",
                    onBack = onBack,
                    backTestTag = "how_to_play_back",
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    RuleStepCard(
                        icon = Icons.Rounded.TouchApp,
                        accent = AccentCyan,
                        title = "1. Connect Two Adjacent Dots",
                        body = "On your turn, press a dot and drag to an adjacent horizontal or vertical dot to draw a line. Diagonal lines and duplicate lines are not allowed.",
                    )
                    RuleStepCard(
                        icon = Icons.Rounded.CheckBox,
                        accent = AccentCoral,
                        title = "2. Complete Squares to Score",
                        body = "Drawing the 4th side of any 1×1 square claims that box in your player color and awards +1 point per box.",
                    )
                    RuleStepCard(
                        icon = Icons.Rounded.Bolt,
                        accent = AccentYellow,
                        title = "3. Earn an Extra Turn",
                        body = "Completing one or more squares on a move grants you exactly one extra turn right away. Otherwise, the turn passes to the next player.",
                    )
                    RuleStepCard(
                        icon = Icons.Rounded.EmojiEvents,
                        accent = AccentPurple,
                        title = "4. Win the Grid",
                        body = "The match ends when every square on the board is claimed. The player with the most boxes wins!",
                    )
                    RuleStepCard(
                        icon = Icons.Rounded.WifiTethering,
                        accent = SuccessEmerald,
                        title = "5. Offline Wi-Fi Hotspot Multiplayer",
                        body = "No internet required! One player enables Android Wi-Fi Hotspot and opens Create Room. Up to 3 friends connect to that hotspot Wi-Fi, tap Join Room, and enter the Host IPv4 address shown on screen.",
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun RuleStepCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    body: String,
) {
    GlassNavyCard(
        borderColor = NavyOutline,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(26.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
    }
}
