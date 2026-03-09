package com.rainbowtimer.ui.setup

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.rainbowtimer.R
import com.rainbowtimer.data.TimerRepository
import com.rainbowtimer.databinding.ActivitySetupBinding
import com.rainbowtimer.ui.timer.TimerActivity
import com.rainbowtimer.util.TimerLogger
import com.rainbowtimer.viewmodel.TimerViewModel

class SetupActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySetupBinding
    private val viewModel: TimerViewModel by viewModels()
    
    private var inputDigits = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupTimeDisplay()
        setupNumberPad()
        setupModeToggle()
        setupStartButton()
        observeViewModel()
    }
    
    private fun setupTimeDisplay() {
        updateTimeDisplay()
    }
    
    private fun updateTimeDisplay() {
        val padded = inputDigits.padStart(4, '0')
        val minutes = padded.substring(0, 2)
        val seconds = padded.substring(2, 4)
        binding.timeDisplay.text = "$minutes:$seconds"
    }
    
    private fun setupNumberPad() {
        val digitButtons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3,
            binding.btn4, binding.btn5, binding.btn6, binding.btn7,
            binding.btn8, binding.btn9
        )
        
        digitButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                if (inputDigits.length < 4) {
                    inputDigits += index.toString()
                    viewModel.setInputDigits(inputDigits)
                    updateTimeDisplay()
                }
            }
        }
        
        binding.btnBackspace.setOnClickListener {
            if (inputDigits.isNotEmpty()) {
                inputDigits = inputDigits.dropLast(1)
                viewModel.setInputDigits(inputDigits)
                updateTimeDisplay()
            }
        }
        
        binding.btnClear.setOnClickListener {
            inputDigits = ""
            viewModel.setInputDigits(inputDigits)
            updateTimeDisplay()
        }
    }
    
    private fun setupModeToggle() {
        updateModeUI(viewModel.currentMode.value ?: TimerRepository.MODE_FIXED_RINGS)
        
        binding.modeFixedRings.setOnClickListener {
            viewModel.setMode(TimerRepository.MODE_FIXED_RINGS)
        }
        
        binding.modeFixedRate.setOnClickListener {
            viewModel.setMode(TimerRepository.MODE_FIXED_RATE)
        }
    }
    
    private fun updateModeUI(mode: Int) {
        when (mode) {
            TimerRepository.MODE_FIXED_RINGS -> {
                binding.modeFixedRings.setTextColor(getColor(android.R.color.black))
                binding.modeFixedRate.setTextColor(getColor(android.R.color.darker_gray))
                binding.modeDescription.text = "20 rings, scales to fit time"
            }
            TimerRepository.MODE_FIXED_RATE -> {
                binding.modeFixedRate.setTextColor(getColor(android.R.color.black))
                binding.modeFixedRings.setTextColor(getColor(android.R.color.darker_gray))
                binding.modeDescription.text = "15 seconds per ring, max 5:00"
            }
        }
    }
    
    private fun setupStartButton() {
        binding.btnStart.setOnClickListener {
            if (viewModel.canStart()) {
                val seconds = viewModel.inputSeconds.value ?: 0
                val mode = viewModel.currentMode.value ?: TimerRepository.MODE_FIXED_RINGS
                
                TimerLogger.d("SetupActivity", "Start clicked - seconds: $seconds, mode: $mode")
                
                startActivity(Intent(this, TimerActivity::class.java).apply {
                    putExtra(TimerActivity.EXTRA_TOTAL_SECONDS, seconds)
                    putExtra(TimerActivity.EXTRA_MODE, mode)
                })
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.inputSeconds.observe(this) { seconds ->
            binding.btnStart.isEnabled = seconds >= 1
        }
        
        viewModel.currentMode.observe(this) { mode ->
            updateModeUI(mode)
        }
        
        viewModel.showMode2CapToast.observe(this) { show ->
            if (show) {
                Toast.makeText(this, "Maximum time in Mode 2 is 5:00", Toast.LENGTH_SHORT).show()
                viewModel.onMode2CapToastShown()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
