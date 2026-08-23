package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Lightweight, zero-dependency audio synthesizer for crisp board sounds,
 * superpower magic rings, capture hits, and victory fanfares.
 */
class SoundManager {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var isMuted = false

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        return isMuted
    }

    fun isAudioMuted(): Boolean = isMuted

    fun playMoveSound() {
        if (isMuted) return
        scope.launch {
            playTone(pitch = 520.0, durationMs = 45, volume = 0.25f, decay = true)
        }
    }

    fun playCaptureSound() {
        if (isMuted) return
        scope.launch {
            playTone(pitch = 280.0, durationMs = 50, volume = 0.4f, decay = true)
            playTone(pitch = 180.0, durationMs = 80, volume = 0.35f, decay = true)
        }
    }

    fun playQueenPromoteSound() {
        if (isMuted) return
        scope.launch {
            playArpeggio(listOf(523.25, 659.25, 783.99, 1046.50), noteDurationMs = 55, volume = 0.35f)
        }
    }

    fun playPowerSound() {
        if (isMuted) return
        scope.launch {
            // Shimmering ascending chime
            playArpeggio(listOf(440.0, 554.37, 659.25, 880.0, 1108.73), noteDurationMs = 40, volume = 0.3f)
        }
    }

    fun playTeleportSound() {
        if (isMuted) return
        scope.launch {
            playGlissando(startFreq = 260.0, endFreq = 920.0, durationMs = 120, volume = 0.35f)
        }
    }

    fun playVictorySound() {
        if (isMuted) return
        scope.launch {
            // Royal Victory Triad
            playTone(pitch = 523.25, durationMs = 120, volume = 0.4f)
            playTone(pitch = 659.25, durationMs = 120, volume = 0.4f)
            playTone(pitch = 783.99, durationMs = 140, volume = 0.45f)
            playTone(pitch = 1046.50, durationMs = 380, volume = 0.5f, decay = true)
        }
    }

    private fun playTone(pitch: Double, durationMs: Int, volume: Float = 0.4f, decay: Boolean = false) {
        try {
            val sampleRate = 44100
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            if (numSamples <= 0) return

            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val env = if (decay) (1.0 - (i.toDouble() / numSamples)) else 1.0
                val sample = (sin(2.0 * Math.PI * pitch * t) * env * Short.MAX_VALUE * volume).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            Thread.sleep(durationMs.toLong() + 20)
            audioTrack.release()
        } catch (_: Exception) {
            // Audio error safety
        }
    }

    private fun playArpeggio(frequencies: List<Double>, noteDurationMs: Int, volume: Float) {
        for (freq in frequencies) {
            playTone(pitch = freq, durationMs = noteDurationMs, volume = volume, decay = true)
        }
    }

    private fun playGlissando(startFreq: Double, endFreq: Double, durationMs: Int, volume: Float) {
        try {
            val sampleRate = 44100
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            if (numSamples <= 0) return

            val buffer = ShortArray(numSamples)
            var phase = 0.0
            for (i in 0 until numSamples) {
                val frac = i.toDouble() / numSamples
                val currentFreq = startFreq + (endFreq - startFreq) * frac
                phase += 2.0 * Math.PI * currentFreq / sampleRate
                val env = (1.0 - frac * 0.4)
                val sample = (sin(phase) * env * Short.MAX_VALUE * volume).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            Thread.sleep(durationMs.toLong() + 20)
            audioTrack.release()
        } catch (_: Exception) {
            // Safe fallback
        }
    }
}
