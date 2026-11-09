package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

class SoundManager(private val context: Context) {

    var soundEnabled: Boolean = true
    var musicEnabled: Boolean = true

    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<String, Int>()
    private var isLoaded = false

    private val audioScope = CoroutineScope(Dispatchers.Default)
    private var musicJob: Job? = null
    private var musicTrack: AudioTrack? = null

    init {
        initializeSoundPool()
    }

    private fun initializeSoundPool() {
        try {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(attributes)
                .build()

            // Pre-generate and cache synthesized WAV sound assets
            audioScope.launch {
                try {
                    val soundDir = File(context.cacheDir, "trapshift_sfx").apply { mkdirs() }

                    val sounds = mapOf(
                        "move" to generateWavBytes(480.0, 45, 0.5f),
                        "wall" to generateWavBytes(140.0, 60, 0.6f),
                        "switch" to generateWavBytes(880.0, 70, 0.5f),
                        "portal" to generateSweepWavBytes(300.0, 1100.0, 120, 0.45f),
                        "trap" to generateWavBytes(200.0, 120, 0.7f, noise = true),
                        "countdown" to generateWavBytes(700.0, 50, 0.4f),
                        "memory" to generateWavBytes(587.33, 80, 0.5f), // D5 chime
                        "victory" to generateFanfareWavBytes(),
                        "failure" to generateDropWavBytes(),
                        "click" to generateWavBytes(640.0, 30, 0.4f)
                    )

                    sounds.forEach { (name, bytes) ->
                        val file = File(soundDir, "$name.wav")
                        if (!file.exists() || file.length() != bytes.size.toLong()) {
                            FileOutputStream(file).use { it.write(bytes) }
                        }
                        soundPool?.let { pool ->
                            val id = pool.load(file.absolutePath, 1)
                            soundIds[name] = id
                        }
                    }
                    isLoaded = true
                } catch (_: Exception) {
                    // Graceful fallback for restricted storage / headless test runner
                }
            }
        } catch (_: Exception) {
            soundPool = null
        }
    }

    private fun play(key: String, volume: Float = 1.0f) {
        if (!soundEnabled) return
        val id = soundIds[key] ?: return
        try {
            soundPool?.play(id, volume, volume, 1, 0, 1.0f)
        } catch (_: Exception) {}
    }

    fun playMove() = play("move", 0.6f)
    fun playWallBump() = play("wall", 0.6f)
    fun playSwitch() = play("switch", 0.7f)
    fun playPortal() = play("portal", 0.7f)
    fun playTrapTrigger() = play("trap", 0.8f)
    fun playCountdownTick() = play("countdown", 0.5f)
    fun playMemoryStep() = play("memory", 0.7f)
    fun playVictory() = play("victory", 0.8f)
    fun playFailure() = play("failure", 0.7f)
    fun playButtonClick() = play("click", 0.5f)

    fun startAmbientMusic() {
        if (!musicEnabled) return
        if (musicJob?.isActive == true) return

        musicJob = audioScope.launch {
            try {
                val sampleRate = 22050
                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(4096)

                musicTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                musicTrack?.play()

                val arpeggio = listOf(220.0, 261.63, 329.63, 392.0, 329.63, 261.63)
                var idx = 0
                val toneDurationMs = 280
                val samplesPerTone = (sampleRate * (toneDurationMs / 1000.0)).toInt()

                while (isActive) {
                    if (musicEnabled) {
                        val freq = arpeggio[idx % arpeggio.size]
                        val buffer = ShortArray(samplesPerTone)
                        for (i in 0 until samplesPerTone) {
                            val t = i.toDouble() / sampleRate
                            val env = 1.0 - (i.toDouble() / samplesPerTone)
                            val sample = (sin(2.0 * PI * freq * t) * 0.12 * env * Short.MAX_VALUE).toInt()
                            buffer[i] = sample.toShort()
                        }
                        musicTrack?.write(buffer, 0, buffer.size)
                        idx++
                        delay(280)
                    } else {
                        delay(500)
                    }
                }
            } catch (_: Exception) {
                // Graceful fallback if streaming audio is unsupported in environment
            }
        }
    }

    fun stopAmbientMusic() {
        musicJob?.cancel()
        musicJob = null
        try {
            musicTrack?.stop()
            musicTrack?.release()
        } catch (_: Exception) {}
        musicTrack = null
    }

    fun release() {
        stopAmbientMusic()
        try {
            soundPool?.release()
        } catch (_: Exception) {}
        soundPool = null
        soundIds.clear()
    }

    // --- Synthesizer WAV File Helpers ---

    private fun generateWavBytes(freq: Double, durationMs: Int, volume: Float, noise: Boolean = false): ByteArray {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcm = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = 1.0 - (i.toDouble() / numSamples)
            val wave = if (noise) {
                (Math.random() * 2 - 1) * envelope
            } else {
                sin(2.0 * PI * freq * t) * envelope
            }
            pcm[i] = (wave * volume * Short.MAX_VALUE).toInt().toShort()
        }

        return createWavContainer(pcm, sampleRate)
    }

    private fun generateSweepWavBytes(startFreq: Double, endFreq: Double, durationMs: Int, volume: Float): ByteArray {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcm = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val fraction = i.toDouble() / numSamples
            val freq = startFreq + (endFreq - startFreq) * fraction
            val t = i.toDouble() / sampleRate
            val envelope = 1.0 - fraction
            val wave = sin(2.0 * PI * freq * t) * envelope
            pcm[i] = (wave * volume * Short.MAX_VALUE).toInt().toShort()
        }

        return createWavContainer(pcm, sampleRate)
    }

    private fun generateFanfareWavBytes(): ByteArray {
        val sampleRate = 22050
        val notes = listOf(523.25, 659.25, 783.99, 1046.50)
        val noteSamples = (sampleRate * 0.08).toInt()
        val totalSamples = noteSamples * notes.size
        val pcm = ShortArray(totalSamples)

        var offset = 0
        for (freq in notes) {
            for (i in 0 until noteSamples) {
                val t = i.toDouble() / sampleRate
                val envelope = 1.0 - (i.toDouble() / noteSamples)
                val wave = sin(2.0 * PI * freq * t) * envelope * 0.7
                pcm[offset + i] = (wave * Short.MAX_VALUE).toInt().toShort()
            }
            offset += noteSamples
        }

        return createWavContainer(pcm, sampleRate)
    }

    private fun generateDropWavBytes(): ByteArray {
        val sampleRate = 22050
        val notes = listOf(350.0, 310.0, 270.0, 220.0)
        val noteSamples = (sampleRate * 0.07).toInt()
        val totalSamples = noteSamples * notes.size
        val pcm = ShortArray(totalSamples)

        var offset = 0
        for (freq in notes) {
            for (i in 0 until noteSamples) {
                val t = i.toDouble() / sampleRate
                val envelope = 1.0 - (i.toDouble() / noteSamples)
                val wave = sin(2.0 * PI * freq * t) * envelope * 0.6
                pcm[offset + i] = (wave * Short.MAX_VALUE).toInt().toShort()
            }
            offset += noteSamples
        }

        return createWavContainer(pcm, sampleRate)
    }

    private fun createWavContainer(pcm: ShortArray, sampleRate: Int): ByteArray {
        val dataSize = pcm.size * 2
        val totalSize = 36 + dataSize
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF Header
        buffer.put("RIFF".toByteArray())
        buffer.putInt(totalSize)
        buffer.put("WAVE".toByteArray())

        // "fmt " chunk
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16) // Subchunk1Size for PCM
        buffer.putShort(1.toShort()) // AudioFormat (1 = PCM)
        buffer.putShort(1.toShort()) // NumChannels (1 = Mono)
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * 2) // ByteRate (SampleRate * NumChannels * BitsPerSample/8)
        buffer.putShort(2.toShort()) // BlockAlign (NumChannels * BitsPerSample/8)
        buffer.putShort(16.toShort()) // BitsPerSample

        // "data" chunk
        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)

        for (sample in pcm) {
            buffer.putShort(sample)
        }

        return buffer.array()
    }
}

class HapticManager(private val context: Context) {
    var vibrationEnabled: Boolean = true

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    fun tick() {
        if (!vibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(20)
            }
        } catch (_: Exception) {}
    }

    fun bump() {
        if (!vibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(40)
            }
        } catch (_: Exception) {}
    }

    fun failure() {
        if (!vibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 70, 50, 90), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (_: Exception) {}
    }

    fun victory() {
        if (!vibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 40, 40, 40, 40, 80), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(100)
            }
        } catch (_: Exception) {}
    }
}
