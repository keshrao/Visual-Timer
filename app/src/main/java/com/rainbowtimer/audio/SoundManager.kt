package com.rainbowtimer.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.rainbowtimer.util.TimerConstants
import com.rainbowtimer.util.TimerLogger
import kotlinx.coroutines.*
import kotlin.math.PI
import kotlin.math.sin

class SoundManager(private val context: Context) {
    
    private var audioTrack: AudioTrack? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var isPlaying = false
    private var playbackJob: Job? = null
    private var volumeRampJob: Job? = null
    
    private val sampleRate = 44100
    private var currentVolume = 0f
    
    private val timerLoggerTag = "SoundManager"

    fun startAlarm() {
        if (isPlaying) {
            return
        }
        
        TimerLogger.d(timerLoggerTag, "Starting alarm")
        
        if (!requestAudioFocus()) {
            TimerLogger.e(timerLoggerTag, "Failed to acquire audio focus")
            return
        }
        
        try {
            startAlarmSound()
            isPlaying = true
        } catch (e: Exception) {
            TimerLogger.e(timerLoggerTag, "Error starting alarm", e)
            releaseAudioFocus()
        }
    }

    private fun requestAudioFocus(): Boolean {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        TimerLogger.d(timerLoggerTag, "Audio focus lost")
                        stopAlarm()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        TimerLogger.d(timerLoggerTag, "Audio focus lost transient")
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        TimerLogger.d(timerLoggerTag, "Audio focus gained")
                    }
                }
            }
            .build()

        val result = audioManager.requestAudioFocus(audioFocusRequest!!)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun startAlarmSound() {
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            android.media.AudioFormat.CHANNEL_OUT_MONO,
            android.media.AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                android.media.AudioFormat.Builder()
                    .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
        
        val ioScope = CoroutineScope(Dispatchers.IO)
        val mainScope = CoroutineScope(Dispatchers.Main)
        
        playbackJob = ioScope.launch {
            playLoopingTone()
        }
        
        volumeRampJob = mainScope.launch {
            rampVolume()
        }
    }

    private suspend fun playLoopingTone() {
        val bufferSize = sampleRate / 10
        val buffer = ShortArray(bufferSize)
        
        while (isPlaying) {
            generateSoftChime(buffer)
            audioTrack?.write(buffer, 0, buffer.size)
        }
    }

    private fun generateSoftChime(buffer: ShortArray) {
        // Bell-like frequencies (harmonics of a bell)
        val frequencies = listOf(523.25, 1046.50, 1569.75, 2093.00)
        val baseAmplitude = 0.4
        
        for (i in buffer.indices) {
            val t = i.toDouble() / sampleRate
            var sample = 0.0
            
            // Bell envelope with attack, sustain, and release
            val attackTime = sampleRate * 0.01  // 10ms attack
            val releaseTime = sampleRate * 0.5  // 500ms release
            val envelope = when {
                i < attackTime -> i.toDouble() / attackTime
                i > buffer.size - releaseTime -> (buffer.size - i).toDouble() / releaseTime
                else -> 1.0
            }
            
            for ((harmIndex, freq) in frequencies.withIndex()) {
                // Decreasing amplitude for higher harmonics (bell-like)
                val harmonicAmplitude = baseAmplitude / (harmIndex + 1)
                // Slightly detuned for richness
                val detune = 1.0 + harmIndex * 0.002
                sample += sin(2.0 * PI * freq * detune * t) * envelope * harmonicAmplitude
            }
            
            sample *= currentVolume
            
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun rampVolume() {
        val steps = 100
        val stepDuration = TimerConstants.ALARM_RAMP_DURATION_MS.toLong() / steps
        
        for (i in 1..steps) {
            if (!isPlaying) break
            currentVolume = easeIn(i.toFloat() / steps)
            audioTrack?.setVolume(currentVolume)
            try {
                Thread.sleep(stepDuration)
            } catch (e: InterruptedException) {
                break
            }
        }
    }

    private fun easeIn(t: Float): Float {
        return t * t
    }

    fun stopAlarm() {
        if (!isPlaying) return
        
        TimerLogger.d(timerLoggerTag, "Stopping alarm")
        
        isPlaying = false
        
        playbackJob?.cancel()
        playbackJob = null
        
        volumeRampJob?.cancel()
        volumeRampJob = null
        
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            TimerLogger.e(timerLoggerTag, "Error stopping audio track", e)
        }
        audioTrack = null
        
        releaseAudioFocus()
        currentVolume = 0f
    }

    private fun releaseAudioFocus() {
        try {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } catch (e: Exception) {
            TimerLogger.e(timerLoggerTag, "Error releasing audio focus", e)
        }
        audioFocusRequest = null
    }

    fun release() {
        stopAlarm()
    }
}
