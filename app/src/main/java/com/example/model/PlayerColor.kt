package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentLime
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.AccentPink
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentSilver
import com.example.ui.theme.AccentYellow

enum class PlayerColor(
    val index: Int,
    val label: String,
    val primary: Color,
    val boxFill: Color,
    val glow: Color,
) {
    CYAN(
        index = 0,
        label = "Cyan",
        primary = AccentCyan,
        boxFill = AccentCyan.copy(alpha = 0.24f),
        glow = AccentCyan.copy(alpha = 0.45f),
    ),
    CORAL(
        index = 1,
        label = "Coral",
        primary = AccentCoral,
        boxFill = AccentCoral.copy(alpha = 0.24f),
        glow = AccentCoral.copy(alpha = 0.45f),
    ),
    YELLOW(
        index = 2,
        label = "Yellow",
        primary = AccentYellow,
        boxFill = AccentYellow.copy(alpha = 0.24f),
        glow = AccentYellow.copy(alpha = 0.45f),
    ),
    PURPLE(
        index = 3,
        label = "Purple",
        primary = AccentPurple,
        boxFill = AccentPurple.copy(alpha = 0.24f),
        glow = AccentPurple.copy(alpha = 0.45f),
    ),
    GREEN(
        index = 4,
        label = "Green",
        primary = AccentGreen,
        boxFill = AccentGreen.copy(alpha = 0.24f),
        glow = AccentGreen.copy(alpha = 0.45f),
    ),
    ORANGE(
        index = 5,
        label = "Orange",
        primary = AccentOrange,
        boxFill = AccentOrange.copy(alpha = 0.24f),
        glow = AccentOrange.copy(alpha = 0.45f),
    ),
    PINK(
        index = 6,
        label = "Pink",
        primary = AccentPink,
        boxFill = AccentPink.copy(alpha = 0.24f),
        glow = AccentPink.copy(alpha = 0.45f),
    ),
    BLUE(
        index = 7,
        label = "Blue",
        primary = AccentBlue,
        boxFill = AccentBlue.copy(alpha = 0.24f),
        glow = AccentBlue.copy(alpha = 0.45f),
    ),
    LIME(
        index = 8,
        label = "Lime",
        primary = AccentLime,
        boxFill = AccentLime.copy(alpha = 0.24f),
        glow = AccentLime.copy(alpha = 0.45f),
    ),
    SILVER(
        index = 9,
        label = "Silver",
        primary = AccentSilver,
        boxFill = AccentSilver.copy(alpha = 0.24f),
        glow = AccentSilver.copy(alpha = 0.45f),
    );

    companion object {
        fun fromIndex(index: Int): PlayerColor {
            return entries.getOrElse(index.coerceIn(0, entries.lastIndex)) { CYAN }
        }
    }
}
