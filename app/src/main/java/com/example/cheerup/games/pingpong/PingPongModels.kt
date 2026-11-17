package com.example.cheerup.games.pingpong

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.cheerup.core.localization.LanguageManager
import com.example.cheerup.domain.model.GameIdentifier
import com.example.cheerup.leaderboard.PlayGamesManager
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

enum class PingPongMode(val rawValue: String) {
    ENDLESS("endless"),
    CLASSIC("classic");

    val id: String get() = rawValue

    val title: String
        get() = when (this) {
            ENDLESS -> LanguageManager.shared.tr("ENDLESS CHALLENGE", "DÉFI SANS FIN", "RETO SIN FIN")
            CLASSIC -> LanguageManager.shared.tr("CLASSIC ARCADE", "ARCADE CLASSIQUE", "CLÁSICO ARCADE")
        }

    val shortTitle: String
        get() = when (this) {
            ENDLESS -> LanguageManager.shared.tr("Endless", "Sans Fin", "Sin Fin")
            CLASSIC -> LanguageManager.shared.tr("Classic (First to 7)", "Classique (7 pts)", "Clásico (a 7 pts)")
        }

    val targetScore: Int
        get() = when (this) {
            ENDLESS -> 999999
            CLASSIC -> 7
        }
}

enum class PingPongPowerUpType(val rawValue: String) {
    GIANT_PADDLE("giantPaddle"),
    SPEED_BOOST("speedBoost"),
    SLOW_MOTION("slowMotion"),
    STICKY_PADDLE("stickyPaddle"),
    DOUBLE_POINTS("doublePoints"),
    SHIELD("shield"),
    CHAOS_BALL("chaosBall"),
    DOUBLE_BALL("doubleBall");

    val id: String get() = rawValue

    val displayName: String
        get() = when (this) {
            GIANT_PADDLE -> LanguageManager.shared.tr("GIANT PADDLE", "RAQUETTE GÉANTE", "PALA GIGANTE")
            SPEED_BOOST -> LanguageManager.shared.tr("SPEED BOOST", "TURBO VITESSE", "TURBO VELOCIDAD")
            SLOW_MOTION -> LanguageManager.shared.tr("SLOW MOTION", "RALENTI", "CÁMARA LENTA")
            STICKY_PADDLE -> LanguageManager.shared.tr("STICKY PADDLE", "RAQUETTE AIMANT", "PALA IMÁN")
            DOUBLE_POINTS -> LanguageManager.shared.tr("2X POINTS", "POINTS DOUBLÉS", "PUNTOS DOBLES")
            SHIELD -> LanguageManager.shared.tr("SHIELD", "BOUCLIER", "ESCUDO")
            CHAOS_BALL -> LanguageManager.shared.tr("CHAOS BALL", "BALLE CHAOS", "BOLA CAOS")
            DOUBLE_BALL -> LanguageManager.shared.tr("DOUBLE BALL", "DOUBLE BALLE", "DOBLE BOLA")
        }

    val shortBadge: String
        get() = when (this) {
            GIANT_PADDLE -> "WIDE"
            SPEED_BOOST -> "FAST"
            SLOW_MOTION -> "SLOW"
            STICKY_PADDLE -> "STICKY"
            DOUBLE_POINTS -> "2X PTS"
            SHIELD -> "SHIELD"
            CHAOS_BALL -> "CHAOS"
            DOUBLE_BALL -> "2X BALL"
        }

    val duration: Double
        get() = when (this) {
            GIANT_PADDLE -> 8.5
            SPEED_BOOST -> 6.5
            SLOW_MOTION -> 6.0
            STICKY_PADDLE -> 8.0
            DOUBLE_POINTS -> 10.0
            SHIELD -> 14.0
            CHAOS_BALL -> 7.0
            DOUBLE_BALL -> 10.0
        }

    val color: Color
        get() = when (this) {
            GIANT_PADDLE -> Color(0xFF2EDB8A)
            SPEED_BOOST -> Color(0xFFFF4B5C)
            SLOW_MOTION -> Color(0xFF3D7BFF)
            STICKY_PADDLE -> Color(0xFFFFC83D)
            DOUBLE_POINTS -> Color(0xFFFF7AB6)
            SHIELD -> Color(0xFF00D2FF)
            CHAOS_BALL -> Color(0xFFA855F7)
            DOUBLE_BALL -> Color(0xFFF5F8FC)
        }

    val uiColorHex: Long
        get() = when (this) {
            GIANT_PADDLE -> 0x2EDB8AL
            SPEED_BOOST -> 0xFF4B5CL
            SLOW_MOTION -> 0x3D7BFFL
            STICKY_PADDLE -> 0xFFC83DL
            DOUBLE_POINTS -> 0xFF7AB6L
            SHIELD -> 0x00D2FFL
            CHAOS_BALL -> 0xA855F7L
            DOUBLE_BALL -> 0xF5F8FCL
        }

    val isPhysicsDisruptive: Boolean
        get() = when (this) {
            SPEED_BOOST, SLOW_MOTION, CHAOS_BALL -> true
            else -> false
        }
}

data class SpawnedPowerUp(
    val id: UUID = UUID.randomUUID(),
    val type: PingPongPowerUpType,
    var position: Offset,
    val radius: Float = 19.0f,
    var lifetime: Double = 8.0,
    var age: Double = 0.0
)

data class ActivePowerUpState(
    val type: PingPongPowerUpType,
    var remaining: Double,
    val totalDuration: Double
) {
    val id: String get() = type.rawValue

    val progress: Double
        get() = if (totalDuration > 0) max(0.0, min(1.0, remaining / totalDuration)) else 0.0
}

enum class PingPongDifficultyTier(val rawValue: Int) {
    ROOKIE(1),
    WARMING_UP(2),
    GETTING_HOT(3),
    BLAZING(4),
    CHAOS_MASTER(5),
    UNSTOPPABLE(6);

    val title: String
        get() = when (this) {
            ROOKIE -> LanguageManager.shared.tr("ROOKIE", "DÉBUTANT", "NOVATO")
            WARMING_UP -> LanguageManager.shared.tr("WARMING UP", "ÉCHAUFFEMENT", "CALENTANDO")
            GETTING_HOT -> LanguageManager.shared.tr("GETTING HOT", "ÇA CHAUFFE", "EN RACHA")
            BLAZING -> LanguageManager.shared.tr("BLAZING", "EXPLOSIF", "ARDIENTE")
            CHAOS_MASTER -> LanguageManager.shared.tr("CHAOS MASTER", "MAÎTRE DU CHAOS", "MAESTRO DEL CAOS")
            UNSTOPPABLE -> LanguageManager.shared.tr("UNSTOPPABLE", "INARRÊTABLE", "IMPARABLE")
        }

    companion object {
        fun forScore(score: Int, rallyCount: Int): PingPongDifficultyTier {
            val metric = score + (rallyCount / 4)
            return when (metric) {
                in 0 until 5 -> ROOKIE
                in 5 until 12 -> WARMING_UP
                in 12 until 22 -> GETTING_HOT
                in 22 until 35 -> BLAZING
                in 35 until 50 -> CHAOS_MASTER
                else -> UNSTOPPABLE
            }
        }
    }
}

sealed class PingPongEvent {
    data class PaddleHit(val isPlayer: Boolean, val position: Offset, val speed: Float, val isSmash: Boolean) : PingPongEvent()
    data class WallBounce(val position: Offset) : PingPongEvent()
    data class ShieldSaved(val position: Offset) : PingPongEvent()
    data class PowerUpSpawned(val item: SpawnedPowerUp) : PingPongEvent()
    data class PowerUpCollected(val type: PingPongPowerUpType, val position: Offset) : PingPongEvent()
    data class PlayerScored(val pointsAdded: Int, val newScore: Int, val position: Offset) : PingPongEvent()
    data class CpuScored(val newCpuScore: Int, val position: Offset) : PingPongEvent()
    data class BallMissedLine(val position: Offset, val byPlayer: Boolean) : PingPongEvent()
    data class StickyCaught(val position: Offset) : PingPongEvent()
    data class StickyLaunched(val position: Offset) : PingPongEvent()
    data class DifficultyIncreased(val tier: PingPongDifficultyTier) : PingPongEvent()
    data class Callout(val text: String, val colorHex: Long) : PingPongEvent()
    data class GameOver(val finalScore: Int, val playerWon: Boolean, val isNewBest: Boolean) : PingPongEvent()
}

class PingPongGameCenterManager private constructor() {
    fun submitScore(score: Int) {
        PlayGamesManager.shared.submitScore(score, GameIdentifier.PING_PONG)
    }

    companion object {
        const val leaderboardID = "com.gc.cheerup.pingpong.highscore"
        val shared: PingPongGameCenterManager by lazy { PingPongGameCenterManager() }
    }
}
