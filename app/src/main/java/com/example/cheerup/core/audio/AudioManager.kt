package com.example.cheerup.core.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.cheerup.R
import com.example.cheerup.core.localization.LanguageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh

class AudioManager private constructor() {

    enum class SoundEffect(val rawName: String) {
        TAP("tap"),
        POP("pop"),
        SWITCH_TOGGLE("switch"),
        SWITCH_OFF("switch_off"),
        BOUNCE("bounce"),
        ERROR("error"),
        NEXT_LEVEL("next_level"),
        COMBO("combo"),
        WIN("win"),
        DING("ding"),
        SLIME_STICK("slime_stick"),
        SUCCESS_TRUMPET("success_trumpet")
    }

    private var prefs: SharedPreferences? = null
    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<SoundEffect, Int>()
    private val soundVolumes = mutableMapOf<SoundEffect, Float>()
    private val audioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var _isEnabled: Boolean by mutableStateOf(true)
    val isEnabled: Boolean
        get() = _isEnabled

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        val sp = appContext.getSharedPreferences(LanguageManager.PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        if (!sp.contains(ENABLED_KEY)) {
            sp.edit().putBoolean(ENABLED_KEY, true).apply()
        }
        _isEnabled = sp.getBoolean(ENABLED_KEY, true)

        audioScope.launch {
            preloadSounds(appContext)
        }
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled = enabled
        prefs?.edit()?.putBoolean(ENABLED_KEY, enabled)?.apply()
    }

    fun play(effect: SoundEffect) {
        if (!isEnabled) return
        val pool = soundPool ?: return
        val id = soundIds[effect] ?: return
        val vol = soundVolumes[effect] ?: 0.85f
        audioScope.launch {
            try {
                pool.play(id, vol, vol, 1, 0, 1.0f)
            } catch (_: Throwable) {
                // Safe no-op if audio device is unavailable
            }
        }
    }

    private fun preloadSounds(context: Context) {
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val pool = SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(attrs)
                .build()
            soundPool = pool

            val rawMap = mapOf(
                SoundEffect.TAP to R.raw.sfx_tap,
                SoundEffect.POP to R.raw.sfx_pop,
                SoundEffect.SWITCH_TOGGLE to R.raw.sfx_switch,
                SoundEffect.SWITCH_OFF to R.raw.sfx_switch_off,
                SoundEffect.BOUNCE to R.raw.sfx_bounce,
                SoundEffect.ERROR to R.raw.sfx_error,
                SoundEffect.NEXT_LEVEL to R.raw.sfx_next_level,
                SoundEffect.COMBO to R.raw.sfx_combo,
                SoundEffect.WIN to R.raw.sfx_win
            )

            for ((effect, resId) in rawMap) {
                soundIds[effect] = pool.load(context, resId, 1)
                soundVolumes[effect] = 0.85f
            }

            // Synthesize the 3 procedural WAV effects (ding, slime_stick, success_trumpet) exactly as on iOS
            for (effect in listOf(SoundEffect.DING, SoundEffect.SLIME_STICK, SoundEffect.SUCCESS_TRUMPET)) {
                val (wavBytes, vol) = synthesizedAudio(effect) ?: continue
                if (wavBytes != null) {
                    val tempFile = File(context.cacheDir, "synth_${effect.rawName}.wav")
                    tempFile.writeBytes(wavBytes)
                    soundIds[effect] = pool.load(tempFile.absolutePath, 1)
                    soundVolumes[effect] = vol
                }
            }
        } catch (_: Throwable) {
            // Safe fallback if SoundPool initialization fails in headless test environments
        }
    }

    companion object {
        private const val ENABLED_KEY = "cheerup.sound.enabled"
        val shared: AudioManager by lazy { AudioManager() }

        fun synthesizedAudio(effect: SoundEffect): Pair<ByteArray?, Float>? {
            return when (effect) {
                SoundEffect.DING -> Pair(makeClockDingWAV(), 0.9f)
                SoundEffect.SLIME_STICK -> Pair(makeSlimeStickWAV(), 0.95f)
                SoundEffect.SUCCESS_TRUMPET -> Pair(makeSuccessTrumpetWAV(), 0.92f)
                else -> null
            }
        }

        private fun makeClockDingWAV(): ByteArray? {
            val sampleRate = 44100
            val duration = 0.95
            val totalSamples = (duration * sampleRate).toInt()
            val pcm = ShortArray(totalSamples)

            val fundamental = 1567.98 // G6 bell pitch
            val overtone1 = 3135.96   // 2x harmonic
            val overtone2 = 4318.00   // Metallic bell chime partial

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                val attack = min(1.0, t / 0.002)
                val env1 = exp(-t * 4.8)
                val env2 = exp(-t * 10.5)
                val env3 = exp(-t * 22.0)

                val s1 = sin(2.0 * PI * fundamental * t) * env1 * 0.62
                val s2 = sin(2.0 * PI * overtone1 * t) * env2 * 0.25
                val s3 = sin(2.0 * PI * overtone2 * t) * env3 * 0.13

                val sample = (s1 + s2 + s3) * attack * 0.85
                pcm[i] = max(-32767.0, min(32767.0, sample * 32767.0)).toInt().toShort()
            }
            return makeMono16WAV(pcm, sampleRate)
        }

        private fun makeSlimeStickWAV(): ByteArray? {
            val sampleRate = 44100
            val duration = 0.19
            val totalSamples = (duration * sampleRate).toInt()
            val pcm = ShortArray(totalSamples)

            var phaseMain = 0.0
            var phaseBubble = 0.0
            var noiseState: Long = 0x5EEDBEEF12345678L
            var lpFilter = 0.0

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                val norm = t / duration
                val attack = min(1.0, t / 0.004)
                val tailFade = if (norm > 0.72) max(0.0, 1.0 - (norm - 0.72) / 0.28) else 1.0

                val mainFreq = if (t < 0.045) {
                    val u = t / 0.045
                    340.0 - 195.0 * u
                } else {
                    val u = min(1.0, (t - 0.045) / 0.115)
                    145.0 + 185.0 * (u * u)
                }
                phaseMain += 2.0 * PI * mainFreq / sampleRate
                val squishEnv = exp(-t * 14.0) * 0.72

                val bubbleT = max(0.0, t - 0.052)
                val bubbleFreq = 220.0 + 360.0 * min(1.0, bubbleT / 0.085)
                phaseBubble += 2.0 * PI * bubbleFreq / sampleRate
                val bubbleWindow = if (t >= 0.052) sin(min(1.0, bubbleT / 0.10) * PI) else 0.0
                val bubble = sin(phaseBubble) * bubbleWindow * 0.45

                noiseState = noiseState * 6364136223846793005L + 1442695040888963407L
                val white = (noiseState shr 32).toDouble() / Int.MAX_VALUE.toDouble()
                lpFilter += 0.13 * (white - lpFilter)
                val ripple = 0.5 + 0.5 * sin(2.0 * PI * 48.0 * t)
                val squelch = lpFilter * exp(-t * 20.0) * ripple * 0.48

                val raw = (sin(phaseMain) * squishEnv + bubble + squelch) * attack * tailFade
                val sample = tanh(raw * 1.15) * 0.90
                pcm[i] = max(-32767.0, min(32767.0, sample * 32767.0)).toInt().toShort()
            }
            return makeMono16WAV(pcm, sampleRate)
        }

        private fun makeSuccessTrumpetWAV(): ByteArray? {
            val sampleRate = 44100
            val duration = 0.78
            val totalSamples = (duration * sampleRate).toInt()
            val pcm = ShortArray(totalSamples)

            data class Note(val startSec: Double, val endSec: Double, val freqHz: Double, val isSustained: Boolean)
            val notes = listOf(
                Note(0.00, 0.11, 523.25, false), // C5
                Note(0.11, 0.22, 659.25, false), // E5
                Note(0.22, 0.34, 783.99, false), // G5
                Note(0.34, 0.78, 1046.50, true)  // C6 triumph hold
            )

            var phase = 0.0
            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                val note = notes.firstOrNull { t >= it.startSec && t < it.endSec } ?: continue

                val noteT = t - note.startSec
                val noteLen = note.endSec - note.startSec
                val attack = min(1.0, noteT / 0.012)
                val releaseLen = if (note.isSustained) 0.14 else 0.025
                val release = min(1.0, max(0.0, (note.endSec - t) / releaseLen))
                val decay = if (note.isSustained) exp(-noteT * 2.2) else exp(-noteT * 5.0)

                val onsetScoop = 1.0 - 0.025 * exp(-noteT * 90.0)
                val vibrato = if (note.isSustained) (1.0 + 0.004 * sin(2.0 * PI * 5.8 * noteT) * min(1.0, noteT / 0.15)) else 1.0
                val freq = note.freqHz * onsetScoop * vibrato
                phase += 2.0 * PI * freq / sampleRate

                val brassBloom = min(1.0, noteT / 0.022)
                val h1 = sin(phase) * 0.48
                val h2 = sin(phase * 2.0) * 0.30 * (0.65 + 0.35 * brassBloom)
                val h3 = sin(phase * 3.0) * 0.19 * brassBloom
                val h4 = sin(phase * 4.0) * 0.11 * brassBloom * exp(-noteT * 4.0)
                val h5 = sin(phase * 5.0) * 0.06 * brassBloom * exp(-noteT * 6.5)

                val env = attack * release * decay * (if (noteLen > 0) 1.0 else 0.0)
                val sample = tanh((h1 + h2 + h3 + h4 + h5) * env * 1.1) * 0.88
                pcm[i] = max(-32767.0, min(32767.0, sample * 32767.0)).toInt().toShort()
            }
            return makeMono16WAV(pcm, sampleRate)
        }

        fun makeMono16WAV(pcm: ShortArray, sampleRate: Int): ByteArray? {
            if (pcm.isEmpty()) return null
            val dataSize = pcm.size * 2
            val fileSize = 36 + dataSize
            val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)

            buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
            buffer.putInt(fileSize)
            buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
            buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
            buffer.putInt(16) // PCM chunk size
            buffer.putShort(1) // PCM format
            buffer.putShort(1) // Mono
            buffer.putInt(sampleRate)
            buffer.putInt(sampleRate * 2) // Byte rate
            buffer.putShort(2) // Block align
            buffer.putShort(16) // Bits per sample
            buffer.put("data".toByteArray(Charsets.US_ASCII))
            buffer.putInt(dataSize)
            for (s in pcm) {
                buffer.putShort(s)
            }
            return buffer.array()
        }
    }
}
