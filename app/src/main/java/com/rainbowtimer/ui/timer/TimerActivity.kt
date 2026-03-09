package com.rainbowtimer.ui.timer

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.rainbowtimer.databinding.ActivityTimerBinding
import com.rainbowtimer.util.TimerLogger
import com.rainbowtimer.viewmodel.TimerViewModel

class TimerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityTimerBinding
    private val viewModel: TimerViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        setupResetButton()
        observeViewModel()
        
        if (savedInstanceState == null) {
            startTimer()
        }
    }
    
    private fun startTimer() {
        val totalSeconds = intent.getIntExtra(EXTRA_TOTAL_SECONDS, 0)
        val mode = intent.getIntExtra(EXTRA_MODE, 1)
        
        TimerLogger.d("TimerActivity", "Received - totalSeconds: $totalSeconds, mode: $mode")
        
        if (totalSeconds > 0) {
            TimerLogger.d("TimerActivity", "Calling setInputDigits with: ${formatSeconds(totalSeconds)}")
            viewModel.setInputDigits(formatSeconds(totalSeconds))
            
            TimerLogger.d("TimerActivity", "Calling setMode with: $mode")
            viewModel.setMode(mode)
            
            TimerLogger.d("TimerActivity", "Calling startTimer")
            viewModel.startTimer()
        } else {
            TimerLogger.e("TimerActivity", "Invalid totalSeconds: $totalSeconds - timer not started!")
        }
    }
    
    private fun formatSeconds(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d%02d", minutes, seconds)
    }
    
    private fun setupResetButton() {
        binding.btnReset.setOnClickListener {
            finish()
        }
    }
    
    private fun observeViewModel() {
        viewModel.timerState.observe(this) { state ->
            TimerLogger.d("TimerActivity", "timerState observer triggered - state: $state")
            state?.let {
                binding.ringTimerView.setState(it)
            }
        }
        
        viewModel.isTimerRunning.observe(this) { running ->
            TimerLogger.d("TimerActivity", "isTimerRunning changed to: $running")
        }
        
        viewModel.isTimerFinished.observe(this) { finished ->
            TimerLogger.d("TimerActivity", "isTimerFinished changed to: $finished")
            if (finished) {
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
    }
    
    override fun onResume() {
        super.onResume()
        if (viewModel.isTimerFinished.value == true) {
            viewModel.resetTimer()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        viewModel.resetTimer()
    }
    
    companion object {
        const val EXTRA_TOTAL_SECONDS = "extra_total_seconds"
        const val EXTRA_MODE = "extra_mode"
    }
}
