package com.example.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class FeedbackManager(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    @Volatile
    var soundEnabled: Boolean = true

    @Volatile
    var vibrationEnabled: Boolean = true

    fun onDotSelected() {
        if (vibrationEnabled) vibratePulse(12L, 70)
        if (soundEnabled) playToneSequence(listOf(520.0 to 28))
    }

    fun onLineDrawn() {
        if (vibrationEnabled) vibratePulse(20L, 120)
        if (soundEnabled) playToneSequence(listOf(660.0 to 45))
    }

    fun onBoxClaimed(isBonus: Boolean) {
        if (vibrationEnabled) {
            if (isBonus) {
                vibratePattern(longArrayOf(0, 28, 35, 42), intArrayOf(0, 160, 0, 220))
            } else {
                vibratePulse(32L, 180)
            }
        }
        if (soundEnabled) {
            playToneSequence(listOf(587.33 to 55, 880.0 to 90))
        }
    }

    fun onVictory() {
        if (vibrationEnabled) {
            vibratePattern(longArrayOf(0, 40, 40, 50, 40, 80), intArrayOf(0, 180, 0, 210, 0, 255))
        }
        if (soundEnabled) {
            playToneSequence(listOf(523.25 to 70, 659.25 to 70, 783.99 to 80, 1046.5 to 150))
        }
    }

    fun onError() {
        if (vibrationEnabled) vibratePulse(40L, 150)
        if (soundEnabled) playToneSequence(listOf(240.0 to 70, 190.0 to 90))
    }

    private fun vibratePulse(durationMs: Long, amplitude: Int) {
        runCatching {
            val vib = vibrator ?: return
            if (!vib.hasVibrator()) return
            vib.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
        }
    }

    private fun vibratePattern(timings: LongArray, amplitudes: IntArray) {
        runCatching {
            val vib = vibrator ?: return
            if (!vib.hasVibrator()) return
            vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        }
    }

    private fun playToneSequence(notes: List<Pair<Double, Int>>) {
        scope.launch {
            runCatching {
                val sampleRate = 22050
                val totalSamples = notes.sumOf { (_, ms) -> (sampleRate * ms) / 1000 }
                if (totalSamples <= 0) return@runCatching
                val buffer = ShortArray(totalSamples)
                var offset = 0
                for ((freq, ms) in notes) {
                    val count = (sampleRate * ms) / 1000
                    for (i in 0 until count) {
                        val t = i.toDouble() / sampleRate
                        val envelope = exp(-4.5 * i.toDouble() / count.coerceAtLeast(1))
                        val wave = sin(2.0 * PI * freq * t) * envelope
                        buffer[offset + i] = (wave * 0.28 * Short.MAX_VALUE).toInt().toShort()
                    }
                    offset += count
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                val totalMs = notes.sumOf { it.second }.toLong() + 60L
                kotlinx.coroutines.delay(totalMs)
                track.stop()
                track.release()
            }
        }
    }
}
