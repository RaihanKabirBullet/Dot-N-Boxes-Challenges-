package com.example.model

enum class BoardSize(
    val dots: Int,
    val displayName: String,
    val tagline: String,
) {
    TINY(3, "Tiny", "Quick Duel"),
    SMALL(4, "Small", "Classic Fast"),
    MEDIUM(5, "Medium", "Balanced Grid"),
    LARGE(6, "Large", "Tactical Match"),
    HUGE(8, "Huge", "Deep Strategy"),
    MASSIVE(10, "Massive", "Marathon Grid");

    val boxesPerSide: Int get() = dots - 1
    val totalBoxes: Int get() = boxesPerSide * boxesPerSide
    val dimensionsLabel: String get() = "${dots}×$dots Dots"
    val boxesLabel: String get() = "$totalBoxes Boxes"

    companion object {
        fun fromNameOrDefault(value: String?): BoardSize {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SMALL
        }
    }
}
