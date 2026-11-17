package com.example.cheerup.presentation.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.cheerup.domain.model.GameIdentifier

class AppState {
    var selectedGame: GameIdentifier? by mutableStateOf(null)
    var showingIntro: Boolean by mutableStateOf(false)
    var isPlayingGame: Boolean by mutableStateOf(false)

    fun selectGame(game: GameIdentifier) {
        selectedGame = game
        showingIntro = true
    }

    fun startGame() {
        showingIntro = false
        isPlayingGame = true
    }

    fun exitToArcade() {
        isPlayingGame = false
        showingIntro = false
        selectedGame = null
    }

    fun showIntro() {
        isPlayingGame = false
        showingIntro = true
    }
}
