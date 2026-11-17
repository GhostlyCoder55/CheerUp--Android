package com.example.cheerup.games.gravity

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.compose.ui.graphics.Color
import com.example.cheerup.core.audio.AudioManager
import com.example.cheerup.core.localization.LanguageManager
import com.example.cheerup.domain.model.GameIdentifier
import com.example.cheerup.leaderboard.PlayGamesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class GridCoord(
    val x: Int,
    val y: Int
)

enum class GravityDirection(val rawValue: Int) {
    UP(0),
    RIGHT(1),
    DOWN(2),
    LEFT(3);

    val dx: Int
        get() = when (this) {
            UP, DOWN -> 0
            RIGHT -> 1
            LEFT -> -1
        }

    val dy: Int
        get() = when (this) {
            UP -> 1
            DOWN -> -1
            LEFT, RIGHT -> 0
        }

    val opposite: GravityDirection
        get() = when (this) {
            UP -> DOWN
            RIGHT -> LEFT
            DOWN -> UP
            LEFT -> RIGHT
        }

    fun rotated(byQuarterTurnsCW: Int): GravityDirection {
        val normalized = ((rawValue + byQuarterTurnsCW) % 4 + 4) % 4
        return entries.firstOrNull { it.rawValue == normalized } ?: UP
    }
}

enum class GravityOrientation(val rawValue: Int) {
    SOUTH(0), // 0°   (Standard: Down is South)
    WEST(1),  // 90°  (Rotated CW: Down is West)
    NORTH(2), // 180° (Inverted: Down is North)
    EAST(3);  // 270° (Rotated CCW: Down is East)

    /** Canvas clockwise rotation in degrees matching SpriteKit zRotationRadians */
    val canvasRotationDegrees: Float
        get() = when (this) {
            SOUTH -> 0f
            WEST -> -90f
            NORTH -> 180f
            EAST -> 90f
        }

    val compassDegrees: Float
        get() = when (this) {
            SOUTH -> 0f
            WEST -> 90f
            NORTH -> 180f
            EAST -> 270f
        }

    val title: String
        get() = when (this) {
            SOUTH -> LanguageManager.shared.tr("SOUTH GRAVITY", "GRAVITÉ SUD", "GRAVEDAD SUR")
            WEST -> LanguageManager.shared.tr("WEST GRAVITY", "GRAVITÉ OUEST", "GRAVEDAD OESTE")
            NORTH -> LanguageManager.shared.tr("NORTH GRAVITY", "GRAVITÉ NORD", "GRAVEDAD NORTE")
            EAST -> LanguageManager.shared.tr("EAST GRAVITY", "GRAVITÉ EST", "GRAVEDAD ESTE")
        }

    fun screenToWorldDirection(screenDir: GravityDirection): GravityDirection =
        screenDir.rotated(byQuarterTurnsCW = rawValue)

    fun shifted(byQuarterTurns: Int): GravityOrientation {
        val next = ((rawValue + byQuarterTurns) % 4 + 4) % 4
        return entries.firstOrNull { it.rawValue == next } ?: SOUTH
    }
}

enum class ElementalMoteType(val rawValue: String) {
    FIRE("fire"),
    WATER("water"),
    EARTH("earth"),
    WIND("wind");

    val id: String get() = rawValue

    val displayName: String
        get() = when (this) {
            FIRE -> LanguageManager.shared.tr("FIRE MOTE", "ORBE DE FEU", "MOTA DE FUEGO")
            WATER -> LanguageManager.shared.tr("WATER MOTE", "ORBE D'EAU", "MOTA DE AGUA")
            EARTH -> LanguageManager.shared.tr("EARTH MOTE", "ORBE DE TERRE", "MOTA DE TIERRA")
            WIND -> LanguageManager.shared.tr("WIND MOTE", "ORBE DE VENT", "MOTA DE VIENTO")
        }

    val shortBadge: String
        get() = when (this) {
            FIRE -> LanguageManager.shared.tr("SPEED + 2X", "VITESSE + 2X", "VELOCIDAD + 2X")
            WATER -> LanguageManager.shared.tr("CALM FLOW", "RALENTI", "CALMA")
            EARTH -> LanguageManager.shared.tr("+1 LENGTH", "+1 TAILLE", "+1 TAMAÑO")
            WIND -> LanguageManager.shared.tr("PHASE BODY", "FANTÔME", "ETÉREO")
        }

    val points: Int
        get() = when (this) {
            FIRE -> 2
            WATER -> 1
            EARTH -> 1
            WIND -> 2
        }

    val growthSegments: Int
        get() = 1

    val effectDuration: Double
        get() = when (this) {
            FIRE -> 5.5
            WATER -> 6.0
            EARTH -> 0.0
            WIND -> 7.5
        }

    val color: Color
        get() = when (this) {
            FIRE -> Color(0xFFF59E0B)
            WATER -> Color(0xFF0284C7)
            EARTH -> Color(0xFF10B981)
            WIND -> Color(0xFFBAE6FD)
        }

    val colorHex: Long
        get() = when (this) {
            FIRE -> 0xF59E0BL
            WATER -> 0x0284C7L
            EARTH -> 0x10B981L
            WIND -> 0xBAE6FDL
        }
}

data class SpawnedMote(
    val id: UUID = UUID.randomUUID(),
    val type: ElementalMoteType,
    var coord: GridCoord
)

data class ActiveElementalEffect(
    val type: ElementalMoteType,
    var remaining: Double,
    val totalDuration: Double
) {
    val id: String get() = type.rawValue

    val progress: Double
        get() = if (totalDuration > 0) max(0.0, min(1.0, remaining / totalDuration)) else 0.0
}

sealed class GravityEvent {
    data object Moved : GravityEvent()
    data class MoteCollected(val type: ElementalMoteType, val coord: GridCoord, val pointsAdded: Int, val newScore: Int) : GravityEvent()
    data class GravityShifted(val newOrientation: GravityOrientation, val crossedEdge: GravityDirection) : GravityEvent()
    data class WindPhasedThroughSelf(val coord: GridCoord) : GravityEvent()
    data class GameOver(val finalScore: Int, val isNewBest: Boolean) : GravityEvent()
}

class GravityGameCenterManager private constructor() {
    fun submitScore(score: Int) {
        PlayGamesManager.shared.submitScore(score, GameIdentifier.GRAVITY)
    }

    companion object {
        const val leaderboardID = "com.gc.cheerup.gravity.highscore"
        val shared: GravityGameCenterManager by lazy { GravityGameCenterManager() }
    }
}

class GravityAudioController private constructor() {

    private var bgmPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null
    private val moteSoundIds = mutableMapOf<ElementalMoteType, Int>()
    private var shiftSoundId: Int = 0
    private var isInitialized = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initializeIfNeeded(context: Context) {
        if (isInitialized) return
        isInitialized = true
        val appContext = context.applicationContext
        scope.launch {
            buildSynthesizedAudio(appContext)
        }
    }

    fun startBackgroundMusic(context: Context? = null) {
        context?.let { initializeIfNeeded(it) }
        if (!AudioManager.shared.isEnabled) {
            pauseBackgroundMusic()
            return
        }
        scope.launch {
            try {
                bgmPlayer?.let { player ->
                    if (!player.isPlaying) {
                        player.seekTo(0)
                        player.start()
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    fun pauseBackgroundMusic() {
        scope.launch {
            try {
                bgmPlayer?.let { if (it.isPlaying) it.pause() }
            } catch (_: Throwable) {}
        }
    }

    fun stopBackgroundMusic() {
        scope.launch {
            try {
                bgmPlayer?.let {
                    if (it.isPlaying) {
                        it.pause()
                        it.seekTo(0)
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    fun playMoteSound(type: ElementalMoteType) {
        if (!AudioManager.shared.isEnabled) return
        scope.launch {
            val id = moteSoundIds[type] ?: 0
            if (id != 0) {
                soundPool?.play(id, 0.85f, 0.85f, 1, 0, 1.0f)
            }
        }
        AudioManager.shared.play(AudioManager.SoundEffect.POP)
    }

    fun playGravityShiftSound() {
        if (!AudioManager.shared.isEnabled) return
        scope.launch {
            if (shiftSoundId != 0) {
                soundPool?.play(shiftSoundId, 0.85f, 0.85f, 1, 0, 1.0f)
            } else {
                AudioManager.shared.play(AudioManager.SoundEffect.SWITCH_TOGGLE)
            }
        }
    }

    private fun buildSynthesizedAudio(context: Context) {
        try {
            val notes = listOf(
                293.66 to 0.28, 349.23 to 0.28, 440.00 to 0.28, 523.25 to 0.28,
                493.88 to 0.28, 440.00 to 0.28, 349.23 to 0.28, 329.63 to 0.28,
                293.66 to 0.28, 392.00 to 0.28, 440.00 to 0.28, 587.33 to 0.28,
                523.25 to 0.28, 440.00 to 0.28, 392.00 to 0.28, 349.23 to 0.28
            )
            makeChiptuneWAV(notes, volume = 0.11)?.let { wav ->
                val bgmFile = File(context.cacheDir, "gravity_bgm.wav")
                bgmFile.writeBytes(wav)
                val mp = MediaPlayer().apply {
                    setDataSource(bgmFile.absolutePath)
                    isLooping = true
                    setVolume(0.55f, 0.55f)
                    prepare()
                }
                bgmPlayer = mp
            }

            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val pool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build()
            soundPool = pool

            val sequences = mapOf(
                ElementalMoteType.FIRE to listOf(523.25 to 0.06, 659.25 to 0.06, 783.99 to 0.09),
                ElementalMoteType.WATER to listOf(587.33 to 0.07, 493.88 to 0.07, 392.00 to 0.10),
                ElementalMoteType.EARTH to listOf(329.63 to 0.07, 440.00 to 0.07, 523.25 to 0.11),
                ElementalMoteType.WIND to listOf(440.00 to 0.05, 554.37 to 0.05, 659.25 to 0.05, 880.00 to 0.09)
            )
            for ((type, seq) in sequences) {
                makeChiptuneWAV(seq, volume = 0.24)?.let { wav ->
                    val f = File(context.cacheDir, "gravity_mote_${type.rawValue}.wav")
                    f.writeBytes(wav)
                    moteSoundIds[type] = pool.load(f.absolutePath, 1)
                }
            }

            val shiftSeq = listOf(349.23 to 0.05, 440.00 to 0.05, 523.25 to 0.08)
            makeChiptuneWAV(shiftSeq, volume = 0.20)?.let { wav ->
                val f = File(context.cacheDir, "gravity_shift.wav")
                f.writeBytes(wav)
                shiftSoundId = pool.load(f.absolutePath, 1)
            }
        } catch (_: Throwable) {}
    }

    companion object {
        val shared: GravityAudioController by lazy { GravityAudioController() }

        private fun makeChiptuneWAV(notes: List<Pair<Double, Double>>, volume: Double): ByteArray? {
            val sampleRate = 22050
            val totalDuration = notes.sumOf { it.second }
            val totalSamples = max(1, (totalDuration * sampleRate).toInt())
            val pcm = ShortArray(totalSamples)
            var sampleIndex = 0

            for ((freq, dur) in notes) {
                val count = (dur * sampleRate).toInt()
                for (i in 0 until count) {
                    if (sampleIndex >= totalSamples) break
                    val t = i.toDouble() / sampleRate
                    val env = min(1.0, i / 140.0) * max(0.0, 1.0 - i.toDouble() / count.toDouble())
                    val phase = (t * freq) % 1.0
                    val triangle = 4.0 * abs(phase - 0.5) - 1.0
                    val square = if (phase < 0.5) 0.35 else -0.35
                    val raw = (triangle * 0.75 + square * 0.25) * env * volume
                    pcm[sampleIndex] = max(-32767.0, min(32767.0, raw * 32767.0)).toInt().toShort()
                    sampleIndex += 1
                }
            }
            return AudioManager.makeMono16WAV(pcm, sampleRate)
        }
    }
}
