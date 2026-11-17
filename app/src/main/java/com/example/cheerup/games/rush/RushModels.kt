package com.example.cheerup.games.rush

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
import kotlin.random.Random

data class RushMeteor(
    val id: UUID = UUID.randomUUID(),
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var rotation: Float,
    var angularVelocity: Float,
    var variantIndex: Int = Random.nextInt(0, 8),
    var hasGrantedNearMiss: Boolean = false,
    var hasPassedShip: Boolean = false
) {
    companion object {
        const val collisionRadius: Float = 19.5f
        const val grazeRadius: Float = 52.0f
    }
}

enum class RushPowerUpType(val rawValue: String) {
    SHIELD("shield"),
    SLOW_TIME("slowTime"),
    FLAME_BOOST("flameBoost");

    val id: String get() = rawValue

    val badgeTitle: String
        get() = when (this) {
            SHIELD -> LanguageManager.shared.tr("SHIELD ORB", "ORBE BOUCLIER", "ORBE ESCUDO")
            SLOW_TIME -> LanguageManager.shared.tr("SLOW ORB", "ORBE RALENTI", "ORBE LENTO")
            FLAME_BOOST -> LanguageManager.shared.tr("FLAME ORB", "ORBE FLAMME", "ORBE LLAMA")
        }

    val duration: Double
        get() = when (this) {
            SHIELD -> 12.0
            SLOW_TIME -> 6.0
            FLAME_BOOST -> 5.0
        }

    val color: Color
        get() = when (this) {
            SHIELD -> Color(0xFF22D3EE)
            SLOW_TIME -> Color(0xFFA855F7)
            FLAME_BOOST -> Color(0xFFFF5500)
        }

    val colorHex: Long
        get() = when (this) {
            SHIELD -> 0x22D3EEL
            SLOW_TIME -> 0xA855F7L
            FLAME_BOOST -> 0xFF5500L
        }
}

data class RushCollectible(
    val id: UUID = UUID.randomUUID(),
    val type: RushPowerUpType,
    var x: Float,
    var y: Float,
    var vy: Float,
    var bobPhase: Double = 0.0
)

data class ActiveRushEffect(
    val type: RushPowerUpType,
    var remaining: Double,
    val totalDuration: Double
) {
    val id: String get() = type.rawValue

    val progress: Double
        get() = if (totalDuration > 0) max(0.0, min(1.0, remaining / totalDuration)) else 0.0
}

enum class RushSpecialEventKind(val rawValue: String) {
    METEOR_SHOWER("meteorShower"),
    TWIN_GATE("twinGate");

    val warningText: String
        get() = when (this) {
            METEOR_SHOWER -> LanguageManager.shared.tr("METEOR WAVE INCOMING!", "VAGUE DE MÉTÉORES !", "¡OLA DE METEOROS!")
            TWIN_GATE -> LanguageManager.shared.tr("METEOR GATE AHEAD!", "PASSAGE DE MÉTÉORES !", "¡PUERTA DE METEOROS!")
        }

    val colorHex: Long
        get() = when (this) {
            METEOR_SHOWER -> 0xFF2E93L
            TWIN_GATE -> 0x22D3EEL
        }
}

sealed class RushEvent {
    data class ScoreChanged(val newScore: Int) : RushEvent()
    data class NearMiss(val x: Float, val y: Float, val streak: Int) : RushEvent()
    data class PowerUpCollected(val type: RushPowerUpType, val x: Float, val y: Float) : RushEvent()
    data class ShieldAbsorbedHit(val x: Float, val y: Float) : RushEvent()
    data class SpecialEventWarning(val kind: RushSpecialEventKind, val safeCorridorX: Float?) : RushEvent()
    data class SpecialEventStarted(val kind: RushSpecialEventKind) : RushEvent()
    data class GameOver(val finalScore: Int, val isNewBest: Boolean, val impactX: Float, val impactY: Float) : RushEvent()
}

class RushGameCenterManager private constructor() {
    fun submitScore(score: Int) {
        PlayGamesManager.shared.submitScore(score, GameIdentifier.RUSH)
    }

    companion object {
        const val leaderboardID = "com.gc.cheerup.rush.highscore"
        val shared: RushGameCenterManager by lazy { RushGameCenterManager() }
    }
}

class RushAudioController private constructor() {

    private var bgmPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null
    private var nearMissId: Int = 0
    private var warningId: Int = 0
    private var shieldBreakId: Int = 0
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

    fun playNearMiss() {
        if (!AudioManager.shared.isEnabled) return
        scope.launch {
            if (nearMissId != 0) {
                soundPool?.play(nearMissId, 0.85f, 0.85f, 1, 0, 1.0f)
            } else {
                AudioManager.shared.play(AudioManager.SoundEffect.POP)
            }
        }
    }

    fun playWarning() {
        if (!AudioManager.shared.isEnabled) return
        scope.launch {
            if (warningId != 0) {
                soundPool?.play(warningId, 0.85f, 0.85f, 1, 0, 1.0f)
            }
        }
    }

    fun playShieldBreak() {
        if (!AudioManager.shared.isEnabled) return
        scope.launch {
            if (shieldBreakId != 0) {
                soundPool?.play(shieldBreakId, 0.9f, 0.9f, 1, 0, 1.0f)
            } else {
                AudioManager.shared.play(AudioManager.SoundEffect.BOUNCE)
            }
        }
    }

    private fun buildSynthesizedAudio(context: Context) {
        try {
            val bgmNotes = listOf(
                220.00 to 0.22, 261.63 to 0.22, 329.63 to 0.22, 392.00 to 0.22,
                329.63 to 0.22, 261.63 to 0.22, 246.94 to 0.22, 293.66 to 0.22,
                196.00 to 0.22, 246.94 to 0.22, 293.66 to 0.22, 392.00 to 0.22,
                329.63 to 0.22, 293.66 to 0.22, 246.94 to 0.22, 220.00 to 0.22
            )
            makeSynthWAV(bgmNotes, volume = 0.09)?.let { wav ->
                val bgmFile = File(context.cacheDir, "rush_bgm.wav")
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

            makeSynthWAV(listOf(587.33 to 0.045, 880.00 to 0.065), volume = 0.22)?.let { wav ->
                val f = File(context.cacheDir, "rush_graze.wav")
                f.writeBytes(wav)
                nearMissId = pool.load(f.absolutePath, 1)
            }
            makeSynthWAV(listOf(440.00 to 0.07, 554.37 to 0.09), volume = 0.20)?.let { wav ->
                val f = File(context.cacheDir, "rush_warn.wav")
                f.writeBytes(wav)
                warningId = pool.load(f.absolutePath, 1)
            }
            makeSynthWAV(listOf(523.25 to 0.06, 392.00 to 0.06, 659.25 to 0.09), volume = 0.25)?.let { wav ->
                val f = File(context.cacheDir, "rush_shield.wav")
                f.writeBytes(wav)
                shieldBreakId = pool.load(f.absolutePath, 1)
            }
        } catch (_: Throwable) {}
    }

    companion object {
        val shared: RushAudioController by lazy { RushAudioController() }

        private fun makeSynthWAV(notes: List<Pair<Double, Double>>, volume: Double): ByteArray? {
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
                    val env = min(1.0, i / 110.0) * max(0.0, 1.0 - i.toDouble() / count.toDouble())
                    val phase = (t * freq) % 1.0
                    val triangle = 4.0 * abs(phase - 0.5) - 1.0
                    val pulse = if (phase < 0.35) 0.4 else -0.4
                    val raw = (triangle * 0.72 + pulse * 0.28) * env * volume
                    pcm[sampleIndex] = max(-32767.0, min(32767.0, raw * 32767.0)).toInt().toShort()
                    sampleIndex += 1
                }
            }
            return AudioManager.makeMono16WAV(pcm, sampleRate)
        }
    }
}
