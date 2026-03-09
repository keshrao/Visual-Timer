package com.rainbowtimer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rainbowtimer.audio.SoundManager
import com.rainbowtimer.data.TimerRepository
import com.rainbowtimer.util.TimerConstants
import com.rainbowtimer.util.TimerLogger
import com.rainbowtimer.view.RingTimerState
import kotlinx.coroutines.*
import kotlin.math.ceil

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = TimerRepository(application)
    private val soundManager = SoundManager(application)
    
    private val timerLoggerTag = "TimerViewModel"
    
    private var timerJob: Job? = null
    private var startTimeMillis: Long = 0
    private var pausedTimeMillis: Long = 0
    private var totalDurationMillis: Long = 0
    private var ringDurationMillis: Long = 0
    
    private val _timerState = MutableLiveData<RingTimerState>()
    val timerState: LiveData<RingTimerState> = _timerState
    
    private val _isTimerRunning = MutableLiveData<Boolean>()
    val isTimerRunning: LiveData<Boolean> = _isTimerRunning
    
    private val _isTimerFinished = MutableLiveData<Boolean>()
    val isTimerFinished: LiveData<Boolean> = _isTimerFinished
    
    private val _inputSeconds = MutableLiveData<Int>()
    val inputSeconds: LiveData<Int> = _inputSeconds
    
    private val _currentMode = MutableLiveData<Int>()
    val currentMode: LiveData<Int> = _currentMode
    
    private val _showMode2CapToast = MutableLiveData<Boolean>()
    val showMode2CapToast: LiveData<Boolean> = _showMode2CapToast
    
    init {
        _currentMode.value = repository.getLastMode()
        _inputSeconds.value = repository.getLastUsedTime()
        _isTimerRunning.value = false
        _isTimerFinished.value = false
    }
    
    fun setInputDigits(digits: String) {
        val seconds = parseDigitsToSeconds(digits)
        
        if (_currentMode.value == TimerRepository.MODE_FIXED_RATE && seconds > TimerConstants.MAX_SECONDS_MODE2) {
            _inputSeconds.value = TimerConstants.MAX_SECONDS_MODE2
            _showMode2CapToast.value = true
        } else {
            _inputSeconds.value = seconds
        }
    }
    
    private fun parseDigitsToSeconds(digits: String): Int {
        if (digits.isEmpty()) return 0
        
        val padded = digits.padStart(4, '0')
        val minutes = padded.substring(0, 2).toIntOrNull() ?: 0
        val seconds = padded.substring(2, 4).toIntOrNull() ?: 0
        
        return minutes * 60 + seconds
    }
    
    fun setMode(mode: Int) {
        _currentMode.value = mode
        repository.saveLastMode(mode)
        
        val currentInput = _inputSeconds.value ?: 0
        if (mode == TimerRepository.MODE_FIXED_RATE && currentInput > TimerConstants.MAX_SECONDS_MODE2) {
            _inputSeconds.value = TimerConstants.MAX_SECONDS_MODE2
            _showMode2CapToast.value = true
        }
    }
    
    fun getRingCount(): Int {
        val mode = _currentMode.value ?: TimerRepository.MODE_FIXED_RINGS
        val seconds = _inputSeconds.value ?: 0
        
        return when (mode) {
            TimerRepository.MODE_FIXED_RINGS -> TimerConstants.MAX_RINGS
            TimerRepository.MODE_FIXED_RATE -> {
                val count = ceil(seconds.toDouble() / TimerConstants.SECONDS_PER_RING_MODE2).toInt()
                count.coerceIn(1, TimerConstants.MAX_RINGS)
            }
            else -> TimerConstants.MAX_RINGS
        }
    }
    
    fun getRingDurationMillis(): Long {
        val mode = _currentMode.value ?: TimerRepository.MODE_FIXED_RINGS
        val seconds = _inputSeconds.value ?: 0
        
        return when (mode) {
            TimerRepository.MODE_FIXED_RINGS -> {
                if (seconds == 0) 0L
                else (seconds * 1000L) / TimerConstants.MAX_RINGS
            }
            TimerRepository.MODE_FIXED_RATE -> (TimerConstants.SECONDS_PER_RING_MODE2 * 1000L)
            else -> 0L
        }
    }
    
    fun canStart(): Boolean {
        val seconds = _inputSeconds.value ?: 0
        return seconds >= TimerConstants.MIN_INPUT_SECONDS
    }
    
    fun startTimer() {
        val seconds = _inputSeconds.value ?: 0
        val mode = _currentMode.value ?: TimerRepository.MODE_FIXED_RINGS
        
        TimerLogger.d(timerLoggerTag, "startTimer() called - seconds: $seconds, mode: $mode")
        
        if (seconds < TimerConstants.MIN_INPUT_SECONDS) {
            TimerLogger.e(timerLoggerTag, "Cannot start timer with $seconds seconds")
            return
        }
        
        repository.saveLastUsedTime(seconds)
        
        totalDurationMillis = seconds * 1000L
        ringDurationMillis = getRingDurationMillis()
        
        TimerLogger.d(timerLoggerTag, "totalDurationMillis: $totalDurationMillis, ringDurationMillis: $ringDurationMillis")
        
        _isTimerFinished.value = false
        _isTimerRunning.value = true
        
        val ringCount = getRingCount()
        TimerLogger.d(timerLoggerTag, "Ring count: $ringCount")
        
        val colors = RingTimerState.generateColors(ringCount)
        
        _timerState.value = RingTimerState(
            ringCount = ringCount,
            activeRingIndex = 0,
            sweepFraction = 0f,
            ringColors = colors
        )
        
        TimerLogger.d(timerLoggerTag, "Timer state initialized, starting coroutine")
        
        startTimeMillis = System.currentTimeMillis()
        
        TimerLogger.d(timerLoggerTag, "Starting timer: ${seconds}s, ringCount: $ringCount, ringDuration: ${ringDurationMillis}ms")
        
        timerJob = viewModelScope.launch {
            runTimer(this)
        }
    }
    
    private suspend fun runTimer(coroutineScope: CoroutineScope) {
        while (coroutineScope.isActive) {
            val elapsedMillis = System.currentTimeMillis() - startTimeMillis
            
            if (elapsedMillis >= totalDurationMillis) {
                finishTimer()
                break
            }
            
            updateTimerState(elapsedMillis)
            delay(TimerConstants.TICK_INTERVAL_MS)
        }
    }
    
    private fun updateTimerState(elapsedMillis: Long) {
        val ringCount = getRingCount()
        val currentRing = (elapsedMillis / ringDurationMillis).toInt().coerceIn(0, ringCount - 1)
        val ringElapsed = elapsedMillis - (currentRing * ringDurationMillis)
        val sweepFraction = (ringElapsed.toFloat() / ringDurationMillis).coerceIn(0f, 1f)
        
        val colors = RingTimerState.generateColors(ringCount)
        
        _timerState.postValue(RingTimerState(
            ringCount = ringCount,
            activeRingIndex = currentRing,
            sweepFraction = sweepFraction,
            ringColors = colors
        ))
    }
    
    private fun finishTimer() {
        _isTimerFinished.value = true
        _isTimerRunning.value = false
        
        _timerState.postValue(RingTimerState(
            ringCount = getRingCount(),
            activeRingIndex = getRingCount() - 1,
            sweepFraction = 1f,
            ringColors = RingTimerState.generateColors(getRingCount())
        ))
        
        TimerLogger.d(timerLoggerTag, "Timer finished, playing alarm")
        soundManager.startAlarm()
    }
    
    fun resetTimer() {
        TimerLogger.d(timerLoggerTag, "Resetting timer")
        
        timerJob?.cancel()
        timerJob = null
        
        soundManager.stopAlarm()
        
        _isTimerRunning.value = false
        _isTimerFinished.value = false
        _timerState.value = null
    }
    
    fun onMode2CapToastShown() {
        _showMode2CapToast.value = false
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        soundManager.release()
    }
}
