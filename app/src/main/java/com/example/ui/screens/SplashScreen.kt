package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.NavyGradientBackdrop
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentYellow
import com.example.ui.theme.BoardDotColor
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
) {
    val lineProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        lineProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        )
        delay(450L)
        onFinished()
    }

    NavyGradientBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onFinished,
                )
                .testTag("splash_screen"),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp),
            ) {
                Canvas(modifier = Modifier.size(136.dp)) {
                    val p = lineProgress.value
                    val pad = size.width * 0.18f
                    val span = size.width - pad * 2f
                    val tl = Offset(pad, pad)
                    val tr = Offset(pad + span, pad)
                    val bl = Offset(pad, pad + span)
                    val br = Offset(pad + span, pad + span)

                    // Box fill fades in near end
                    val boxAlpha = ((p - 0.55f) / 0.45f).coerceIn(0f, 1f) * 0.24f
                    if (boxAlpha > 0f) {
                        drawRoundRect(
                            color = AccentCyan.copy(alpha = boxAlpha),
                            topLeft = Offset(pad + 8f, pad + 8f),
                            size = Size(span - 16f, span - 16f),
                            cornerRadius = CornerRadius(18f, 18f),
                        )
                    }

                    val stroke = 12f
                    // Top Cyan
                    val s1 = (p * 4f).coerceIn(0f, 1f)
                    if (s1 > 0f) {
                        drawLine(
                            color = AccentCyan,
                            start = tl,
                            end = Offset(tl.x + span * s1, tl.y),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round,
                        )
                    }
                    // Right Coral
                    val s2 = ((p - 0.25f) * 4f).coerceIn(0f, 1f)
                    if (s2 > 0f) {
                        drawLine(
                            color = AccentCoral,
                            start = tr,
                            end = Offset(tr.x, tr.y + span * s2),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round,
                        )
                    }
                    // Bottom Yellow
                    val s3 = ((p - 0.50f) * 4f).coerceIn(0f, 1f)
                    if (s3 > 0f) {
                        drawLine(
                            color = AccentYellow,
                            start = br,
                            end = Offset(br.x - span * s3, br.y),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round,
                        )
                    }
                    // Left Purple
                    val s4 = ((p - 0.75f) * 4f).coerceIn(0f, 1f)
                    if (s4 > 0f) {
                        drawLine(
                            color = AccentPurple,
                            start = bl,
                            end = Offset(bl.x, bl.y - span * s4),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round,
                        )
                    }

                    listOf(tl, tr, bl, br).forEach { pt ->
                        drawCircle(color = BoardDotColor, radius = 14f, center = pt)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "DOT N BOX CHALLENGE",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Offline Hotspot & Local Multiplayer",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp, start = 24.dp, end = 24.dp),
            ) {
                Text(
                    text = "Created By Raihan Kabir",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = AccentCyan.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("splash_creator_credit"),
                )
            }
        }
    }
}
