package com.example.ui.navigation

object NavRoutes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val PLAY_MENU = "play_menu"
    const val ON_BOARD_SETUP = "on_board_setup"
    const val CREATE_ROOM = "create_room"
    const val JOIN_ROOM = "join_room"
    const val LOBBY = "lobby"
    const val BOARD_SELECTION = "board_selection/{source}"
    const val GAME = "game"
    const val PAUSE = "pause"
    const val RESULTS = "results"
    const val HOW_TO_PLAY = "how_to_play"

    fun boardSelection(source: String): String = "board_selection/$source"
}
