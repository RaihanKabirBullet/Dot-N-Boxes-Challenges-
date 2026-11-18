package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.game.DotsAndBoxesEngine
import com.example.model.DotCoord
import com.example.model.GamePhase
import com.example.model.GameState
import com.example.model.LineId
import com.example.model.LineType
import com.example.model.PlayerColor
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BoardDotColor
import com.example.ui.theme.BoardGhostLine
import com.example.ui.theme.BoardSurface
import com.example.ui.theme.LastMoveRed
import com.example.ui.theme.NavyOutline
import com.example.ui.theme.SpaceGroteskFontFamily
import kotlinx.coroutines.launch
import kotlin.math.hypot

@Composable
fun DotsAndBoxesBoard(
    gameState: GameState,
    localPlayerId: String,
    isMyTurn: Boolean,
    onDotSelectedFeedback: () -> Unit,
    onInvalidTapFeedback: () -> Unit,
    onLineCommitted: (LineId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dots = gameState.boardSize.dots
    val boxDim = gameState.boardSize.boxesPerSide
    val activeColor = gameState.currentPlayer?.color?.primary ?: AccentCyan

    var dragStartDot by remember(gameState.boardSize, gameState.moveSequence) {
        mutableStateOf<DotCoord?>(null)
    }
    var dragTargetDot by remember(gameState.boardSize, gameState.moveSequence) {
        mutableStateOf<DotCoord?>(null)
    }
    var dragCurrentOffset by remember(gameState.boardSize, gameState.moveSequence) {
        mutableStateOf<Offset?>(null)
    }

    val latestState by rememberUpdatedState(gameState)
    val latestIsMyTurn by rememberUpdatedState(isMyTurn)

    // Animated line progress (0f -> 1f) keyed by LineId.key
    val lineAnimations = remember(gameState.boardSize) {
        mutableStateMapOf<String, Animatable<Float, AnimationVector1D>>()
    }
    // Animated box pop scale (0.3f -> 1f) keyed by box index
    val boxAnimations = remember(gameState.boardSize) {
        mutableStateMapOf<Int, Animatable<Float, AnimationVector1D>>()
    }

    LaunchedEffect(gameState.moveSequence, gameState.horizontalLines, gameState.verticalLines, gameState.boxes) {
        if (gameState.moveSequence == 0L) {
            lineAnimations.clear()
            boxAnimations.clear()
        }
        // Trigger line draw-in animations for newly drawn lines
        for (r in 0 until dots) {
            for (c in 0 until boxDim) {
                val owner = gameState.horizontalLines[r * boxDim + c]
                if (owner >= 0) {
                    val key = LineId(LineType.HORIZONTAL, r, c).key
                    if (!lineAnimations.containsKey(key)) {
                        val anim = Animatable(0f)
                        lineAnimations[key] = anim
                        launch {
                            anim.animateTo(1f, tween(durationMillis = 220, easing = FastOutSlowInEasing))
                        }
                    }
                }
            }
        }
        for (r in 0 until boxDim) {
            for (c in 0 until dots) {
                val owner = gameState.verticalLines[r * dots + c]
                if (owner >= 0) {
                    val key = LineId(LineType.VERTICAL, r, c).key
                    if (!lineAnimations.containsKey(key)) {
                        val anim = Animatable(0f)
                        lineAnimations[key] = anim
                        launch {
                            anim.animateTo(1f, tween(durationMillis = 220, easing = FastOutSlowInEasing))
                        }
                    }
                }
            }
        }
        for (idx in gameState.boxes.indices) {
            if (gameState.boxes[idx] >= 0 && !boxAnimations.containsKey(idx)) {
                val anim = Animatable(0.35f)
                boxAnimations[idx] = anim
                launch {
                    anim.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 420f))
                }
            }
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "dot_pulse")
    val pulseProgress by pulseTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )

    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val boardSide = min(maxWidth, maxHeight)

        Surface(
            modifier = Modifier
                .size(boardSide)
                .testTag("game_board_canvas_surface")
                .semantics { contentDescription = "Dot N Box Challenge ${dots} by $dots game board" },
            shape = RoundedCornerShape(26.dp),
            color = BoardSurface,
            border = BorderStroke(
                width = 1.5.dp,
                color = if (isMyTurn && gameState.phase == GamePhase.IN_PROGRESS) {
                    activeColor.copy(alpha = 0.45f)
                } else {
                    NavyOutline
                },
            ),
            tonalElevation = 6.dp,
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .testTag("game_board_canvas")
                    .pointerInput(dots) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val state = latestState
                            if (state.phase != GamePhase.IN_PROGRESS || !latestIsMyTurn) {
                                onInvalidTapFeedback()
                                return@awaitEachGesture
                            }

                            val canvasMin = minOf(size.width.toFloat(), size.height.toFloat())
                            val padding = (canvasMin * 0.085f).coerceIn(22f, 52f)
                            val gridSpan = (canvasMin - padding * 2f).coerceAtLeast(1f)
                            val step = gridSpan / (dots - 1).coerceAtLeast(1)
                            val originX = (size.width - gridSpan) / 2f
                            val originY = (size.height - gridSpan) / 2f
                            val hitRadius = (step * 0.48f).coerceAtLeast(28f)

                            fun dotCenter(d: DotCoord): Offset =
                                Offset(originX + d.col * step, originY + d.row * step)

                            var startDot: DotCoord? = null
                            var bestDist = Float.MAX_VALUE
                            for (r in 0 until dots) {
                                for (c in 0 until dots) {
                                    val dx = originX + c * step - down.position.x
                                    val dy = originY + r * step - down.position.y
                                    val dist = hypot(dx, dy)
                                    if (dist <= hitRadius && dist < bestDist) {
                                        bestDist = dist
                                        startDot = DotCoord(r, c)
                                    }
                                }
                            }

                            val anchorDot = startDot ?: return@awaitEachGesture
                            dragStartDot = anchorDot
                            dragTargetDot = null
                            dragCurrentOffset = down.position
                            onDotSelectedFeedback()

                            val anchorPos = dotCenter(anchorDot)
                            var attemptedDrawnLine: LineId? = null
                            var cancelled = false

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                    ?: event.changes.firstOrNull()
                                if (change == null) {
                                    cancelled = true
                                    break
                                }
                                if (!change.pressed) {
                                    change.consume()
                                    break
                                }
                                change.consume()

                                val pos = change.position
                                dragCurrentOffset = pos

                                val dx = pos.x - anchorPos.x
                                val dy = pos.y - anchorPos.y
                                val distFromAnchor = hypot(dx, dy)

                                // Determine orthogonal neighbor target when dragged sufficiently from start dot
                                var candidateTarget: DotCoord? = null
                                val minDragThreshold = step * 0.36f
                                if (distFromAnchor >= minDragThreshold) {
                                    val absX = kotlin.math.abs(dx)
                                    val absY = kotlin.math.abs(dy)
                                    val candidate = when {
                                        absX >= absY * 1.25f && dx > 0f -> DotCoord(anchorDot.row, anchorDot.col + 1)
                                        absX >= absY * 1.25f && dx < 0f -> DotCoord(anchorDot.row, anchorDot.col - 1)
                                        absY >= absX * 1.25f && dy > 0f -> DotCoord(anchorDot.row + 1, anchorDot.col)
                                        absY >= absX * 1.25f && dy < 0f -> DotCoord(anchorDot.row - 1, anchorDot.col)
                                        else -> {
                                            // Fallback: direct proximity to any orthogonal neighbor
                                            listOf(
                                                DotCoord(anchorDot.row - 1, anchorDot.col),
                                                DotCoord(anchorDot.row + 1, anchorDot.col),
                                                DotCoord(anchorDot.row, anchorDot.col - 1),
                                                DotCoord(anchorDot.row, anchorDot.col + 1),
                                            ).filter { n ->
                                                n.row in 0 until dots && n.col in 0 until dots
                                            }.minByOrNull { n ->
                                                val nc = dotCenter(n)
                                                hypot(nc.x - pos.x, nc.y - pos.y)
                                            }?.takeIf { n ->
                                                val nc = dotCenter(n)
                                                hypot(nc.x - pos.x, nc.y - pos.y) <= hitRadius
                                            }
                                        }
                                    }

                                    if (candidate != null && candidate.row in 0 until dots && candidate.col in 0 until dots) {
                                        val line = LineId.fromDots(anchorDot, candidate)
                                        if (line != null && !DotsAndBoxesEngine.isLineDrawn(latestState, line)) {
                                            candidateTarget = candidate
                                            attemptedDrawnLine = null
                                        } else {
                                            attemptedDrawnLine = line
                                        }
                                    }
                                } else {
                                    attemptedDrawnLine = null
                                }

                                dragTargetDot = candidateTarget
                            }

                            val finalTarget = dragTargetDot
                            if (!cancelled && finalTarget != null) {
                                val line = LineId.fromDots(anchorDot, finalTarget)
                                if (line != null && !DotsAndBoxesEngine.isLineDrawn(latestState, line)) {
                                    onLineCommitted(line)
                                } else {
                                    onInvalidTapFeedback()
                                }
                            } else if (!cancelled && attemptedDrawnLine != null) {
                                onInvalidTapFeedback()
                            }

                            dragStartDot = null
                            dragTargetDot = null
                            dragCurrentOffset = null
                        }
                    },
            ) {
                val canvasMin = minOf(size.width, size.height)
                val padding = (canvasMin * 0.085f).coerceIn(22f, 52f)
                val gridSpan = (canvasMin - padding * 2f).coerceAtLeast(1f)
                val step = gridSpan / (dots - 1).coerceAtLeast(1)
                val originX = (size.width - gridSpan) / 2f
                val originY = (size.height - gridSpan) / 2f

                fun dotPos(r: Int, c: Int): Offset = Offset(originX + c * step, originY + r * step)

                val lineStroke = (step * 0.14f).coerceIn(5f, 15f)
                val dotRadius = (step * 0.14f).coerceIn(5.5f, 15f)

                // 1. Claimed boxes with pop animation & player initial
                for (br in 0 until boxDim) {
                    for (bc in 0 until boxDim) {
                        val boxIdx = br * boxDim + bc
                        val ownerIndex = gameState.boxes.getOrNull(boxIdx) ?: -1
                        if (ownerIndex >= 0) {
                            val owner = gameState.players.getOrNull(ownerIndex)
                            val pColor = owner?.color ?: PlayerColor.fromIndex(ownerIndex)
                            val scale = (boxAnimations[boxIdx]?.value ?: 1f).coerceIn(0.2f, 1.05f)
                            val boxInset = step * 0.12f
                            val fullSide = (step - boxInset * 2f).coerceAtLeast(4f)
                            val scaledSide = fullSide * scale
                            val center = Offset(
                                originX + (bc + 0.5f) * step,
                                originY + (br + 0.5f) * step,
                            )
                            val topLeft = Offset(center.x - scaledSide / 2f, center.y - scaledSide / 2f)

                            drawRoundRect(
                                color = pColor.boxFill,
                                topLeft = topLeft,
                                size = Size(scaledSide, scaledSide),
                                cornerRadius = CornerRadius(step * 0.14f, step * 0.14f),
                            )
                            drawRoundRect(
                                color = pColor.primary.copy(alpha = 0.5f),
                                topLeft = topLeft,
                                size = Size(scaledSide, scaledSide),
                                cornerRadius = CornerRadius(step * 0.14f, step * 0.14f),
                                style = Stroke(width = 2f),
                            )

                            val label = owner?.initial ?: "P"
                            val fontSizeSp = (step * 0.28f / density).coerceIn(11f, 28f).sp
                            val textResult = textMeasurer.measure(
                                text = label,
                                style = TextStyle(
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = fontSizeSp,
                                    color = pColor.primary,
                                ),
                            )
                            drawText(
                                textLayoutResult = textResult,
                                topLeft = Offset(
                                    center.x - textResult.size.width / 2f,
                                    center.y - textResult.size.height / 2f,
                                ),
                            )
                        }
                    }
                }

                // 2. Subtle guide grid lines for un-drawn edges
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(step * 0.10f, step * 0.10f), 0f)
                for (r in 0 until dots) {
                    for (c in 0 until boxDim) {
                        if (gameState.horizontalLines.getOrNull(r * boxDim + c) == -1) {
                            drawLine(
                                color = BoardGhostLine,
                                start = dotPos(r, c),
                                end = dotPos(r, c + 1),
                                strokeWidth = (lineStroke * 0.32f).coerceAtLeast(2f),
                                cap = StrokeCap.Round,
                                pathEffect = dashEffect,
                            )
                        }
                    }
                }
                for (r in 0 until boxDim) {
                    for (c in 0 until dots) {
                        if (gameState.verticalLines.getOrNull(r * dots + c) == -1) {
                            drawLine(
                                color = BoardGhostLine,
                                start = dotPos(r, c),
                                end = dotPos(r + 1, c),
                                strokeWidth = (lineStroke * 0.32f).coerceAtLeast(2f),
                                cap = StrokeCap.Round,
                                pathEffect = dashEffect,
                            )
                        }
                    }
                }

                // 3. Drawn horizontal & vertical lines (with animated draw-in & last-opponent-move RED highlight)
                val lastMove = gameState.lastMove
                for (r in 0 until dots) {
                    for (c in 0 until boxDim) {
                        val ownerIndex = gameState.horizontalLines.getOrNull(r * boxDim + c) ?: -1
                        if (ownerIndex >= 0) {
                            val lineId = LineId(LineType.HORIZONTAL, r, c)
                            val pColor = gameState.players.getOrNull(ownerIndex)?.color
                                ?: PlayerColor.fromIndex(ownerIndex)
                            val progress = lineAnimations[lineId.key]?.value ?: 1f
                            val start = dotPos(r, c)
                            val end = dotPos(r, c + 1)
                            val animEnd = Offset(
                                start.x + (end.x - start.x) * progress,
                                start.y + (end.y - start.y) * progress,
                            )
                            val isLastAnyMove = lastMove?.lineKey == lineId.key
                            val isLastOpponentMove = isLastAnyMove && lastMove?.playerId != localPlayerId
                            val strokeColor = if (isLastOpponentMove) LastMoveRed else pColor.primary
                            if (isLastOpponentMove) {
                                drawLine(
                                    color = LastMoveRed.copy(alpha = 0.45f),
                                    start = start,
                                    end = animEnd,
                                    strokeWidth = lineStroke * 2.2f,
                                    cap = StrokeCap.Round,
                                )
                            } else if (isLastAnyMove) {
                                drawLine(
                                    color = pColor.glow,
                                    start = start,
                                    end = animEnd,
                                    strokeWidth = lineStroke * 1.9f,
                                    cap = StrokeCap.Round,
                                )
                            }
                            drawLine(
                                color = strokeColor,
                                start = start,
                                end = animEnd,
                                strokeWidth = lineStroke,
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                }

                for (r in 0 until boxDim) {
                    for (c in 0 until dots) {
                        val ownerIndex = gameState.verticalLines.getOrNull(r * dots + c) ?: -1
                        if (ownerIndex >= 0) {
                            val lineId = LineId(LineType.VERTICAL, r, c)
                            val pColor = gameState.players.getOrNull(ownerIndex)?.color
                                ?: PlayerColor.fromIndex(ownerIndex)
                            val progress = lineAnimations[lineId.key]?.value ?: 1f
                            val start = dotPos(r, c)
                            val end = dotPos(r + 1, c)
                            val animEnd = Offset(
                                start.x + (end.x - start.x) * progress,
                                start.y + (end.y - start.y) * progress,
                            )
                            val isLastAnyMove = lastMove?.lineKey == lineId.key
                            val isLastOpponentMove = isLastAnyMove && lastMove?.playerId != localPlayerId
                            val strokeColor = if (isLastOpponentMove) LastMoveRed else pColor.primary
                            if (isLastOpponentMove) {
                                drawLine(
                                    color = LastMoveRed.copy(alpha = 0.45f),
                                    start = start,
                                    end = animEnd,
                                    strokeWidth = lineStroke * 2.2f,
                                    cap = StrokeCap.Round,
                                )
                            } else if (isLastAnyMove) {
                                drawLine(
                                    color = pColor.glow,
                                    start = start,
                                    end = animEnd,
                                    strokeWidth = lineStroke * 1.9f,
                                    cap = StrokeCap.Round,
                                )
                            }
                            drawLine(
                                color = strokeColor,
                                start = start,
                                end = animEnd,
                                strokeWidth = lineStroke,
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                }

                // 4. Candidate neighbor hints & live drag preview line
                val sel = dragStartDot
                val snapTarget = dragTargetDot
                val curOffset = dragCurrentOffset
                if (sel != null && isMyTurn && gameState.phase == GamePhase.IN_PROGRESS) {
                    val neighbors = listOf(
                        DotCoord(sel.row - 1, sel.col),
                        DotCoord(sel.row + 1, sel.col),
                        DotCoord(sel.row, sel.col - 1),
                        DotCoord(sel.row, sel.col + 1),
                    ).filter { n ->
                        n.row in 0 until dots && n.col in 0 until dots &&
                            LineId.fromDots(sel, n)?.let { !DotsAndBoxesEngine.isLineDrawn(gameState, it) } == true
                    }

                    val selCenter = dotPos(sel.row, sel.col)
                    for (n in neighbors) {
                        val nCenter = dotPos(n.row, n.col)
                        drawLine(
                            color = activeColor.copy(alpha = 0.28f),
                            start = selCenter,
                            end = nCenter,
                            strokeWidth = lineStroke * 0.6f,
                            cap = StrokeCap.Round,
                            pathEffect = dashEffect,
                        )
                        drawCircle(
                            color = activeColor.copy(alpha = 0.35f),
                            radius = dotRadius * 1.85f * pulseProgress,
                            center = nCenter,
                            style = Stroke(width = 2.5f),
                        )
                    }

                    if (snapTarget != null) {
                        val targetCenter = dotPos(snapTarget.row, snapTarget.col)
                        drawLine(
                            color = activeColor.copy(alpha = 0.38f),
                            start = selCenter,
                            end = targetCenter,
                            strokeWidth = lineStroke * 1.9f,
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = activeColor,
                            start = selCenter,
                            end = targetCenter,
                            strokeWidth = lineStroke,
                            cap = StrokeCap.Round,
                        )
                        drawCircle(
                            color = activeColor,
                            radius = dotRadius * 1.45f,
                            center = targetCenter,
                        )
                    } else if (curOffset != null) {
                        drawLine(
                            color = activeColor.copy(alpha = 0.72f),
                            start = selCenter,
                            end = curOffset,
                            strokeWidth = lineStroke * 0.85f,
                            cap = StrokeCap.Round,
                        )
                    }
                }

                // 5. Dots on top
                for (r in 0 until dots) {
                    for (c in 0 until dots) {
                        val center = dotPos(r, c)
                        val isSelected = (sel?.row == r && sel.col == c) ||
                            (snapTarget?.row == r && snapTarget.col == c)
                        if (isSelected) {
                            drawCircle(
                                color = activeColor.copy(alpha = 0.35f),
                                radius = dotRadius * 2.25f * pulseProgress,
                                center = center,
                            )
                            drawCircle(
                                color = activeColor,
                                radius = dotRadius * 1.45f,
                                center = center,
                            )
                        } else {
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.35f),
                                radius = dotRadius * 1.25f,
                                center = center,
                            )
                            drawCircle(
                                color = BoardDotColor,
                                radius = dotRadius,
                                center = center,
                            )
                        }
                    }
                }
            }
        }
    }
}
