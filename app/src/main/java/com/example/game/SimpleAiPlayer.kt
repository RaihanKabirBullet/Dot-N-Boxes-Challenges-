package com.example.game

import com.example.model.GameState
import com.example.model.LineId
import kotlin.random.Random

object SimpleAiPlayer {

    fun chooseMove(state: GameState, random: Random = Random.Default): LineId? {
        val candidates = DotsAndBoxesEngine.availableLines(state)
        if (candidates.isEmpty()) return null

        val dots = state.boardSize.dots
        val boxDim = state.boardSize.boxesPerSide

        val completingMoves = mutableListOf<LineId>()
        val safeMoves = mutableListOf<LineId>()
        val riskyMoves = mutableListOf<LineId>()

        for (line in candidates) {
            val adjBoxes = DotsAndBoxesEngine.adjacentBoxCoords(line, boxDim)
            var completesABox = false
            var createsThirdSide = false

            for ((br, bc) in adjBoxes) {
                val drawnSides = DotsAndBoxesEngine.countDrawnSidesForBox(
                    br = br,
                    bc = bc,
                    dots = dots,
                    boxDim = boxDim,
                    horizontal = state.horizontalLines,
                    vertical = state.verticalLines,
                )
                if (drawnSides == 3) {
                    completesABox = true
                } else if (drawnSides == 2) {
                    createsThirdSide = true
                }
            }

            when {
                completesABox -> completingMoves.add(line)
                !createsThirdSide -> safeMoves.add(line)
                else -> riskyMoves.add(line)
            }
        }

        return when {
            completingMoves.isNotEmpty() -> completingMoves.random(random)
            safeMoves.isNotEmpty() -> safeMoves.random(random)
            else -> riskyMoves.random(random)
        }
    }
}
