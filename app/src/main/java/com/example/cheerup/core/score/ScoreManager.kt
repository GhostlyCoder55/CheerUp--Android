package com.example.cheerup.core.score

import android.content.Context
import android.content.SharedPreferences
import com.example.cheerup.core.localization.LanguageManager
import com.example.cheerup.domain.model.GameIdentifier
import com.example.cheerup.games.chess.PieceColor
import com.example.cheerup.leaderboard.PlayGamesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class ScoreManager private constructor() {

    private var prefs: SharedPreferences? = null
    private val memoryFallback = mutableMapOf<String, Int>()
    private val recordedChessMatchIDs = mutableSetOf<UUID>()

    private val _scoresFlow = MutableStateFlow<Map<GameIdentifier, Int>>(emptyMap())
    val scoresFlow: StateFlow<Map<GameIdentifier, Int>> = _scoresFlow.asStateFlow()

    fun initialize(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(LanguageManager.PREFS_NAME, Context.MODE_PRIVATE)
        refreshFlow()
    }

    fun highScore(forGame: GameIdentifier): Int {
        val key = PREFIX + forGame.rawValue
        val sp = prefs
        return if (sp != null) {
            sp.getInt(key, memoryFallback[key] ?: 0)
        } else {
            memoryFallback[key] ?: 0
        }
    }

    fun updateHighScore(forGame: GameIdentifier, score: Int) {
        val current = highScore(forGame)
        if (score > current) {
            val key = PREFIX + forGame.rawValue
            memoryFallback[key] = score
            prefs?.edit()?.putInt(key, score)?.apply()
            refreshFlow()
        }
    }

    fun setHighScoreForTesting(forGame: GameIdentifier, score: Int) {
        val key = PREFIX + forGame.rawValue
        memoryFallback[key] = score
        prefs?.edit()?.putInt(key, score)?.apply()
        refreshFlow()
    }

    fun recordChessOutcome(
        winner: PieceColor?,
        humanColor: PieceColor = PieceColor.LIGHT,
        matchID: UUID? = null
    ): Int {
        val currentWins = highScore(GameIdentifier.CHESS)
        if (matchID != null) {
            synchronized(recordedChessMatchIDs) {
                if (recordedChessMatchIDs.contains(matchID)) {
                    return currentWins
                }
                recordedChessMatchIDs.add(matchID)
            }
        }
        if (winner == humanColor) {
            val updatedWins = currentWins + 1
            updateHighScore(GameIdentifier.CHESS, updatedWins)
            PlayGamesManager.shared.submitScore(updatedWins, GameIdentifier.CHESS)
            return updatedWins
        }
        return currentWins
    }

    fun allHighScores(): Map<GameIdentifier, Int> {
        val map = mutableMapOf<GameIdentifier, Int>()
        for (game in GameIdentifier.entries) {
            map[game] = highScore(game)
        }
        return map
    }

    private fun refreshFlow() {
        _scoresFlow.value = allHighScores()
    }

    companion object {
        private const val PREFIX = "cheerup.highscore."
        val shared: ScoreManager by lazy { ScoreManager() }
    }
}
