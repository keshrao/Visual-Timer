package com.rainbowtimer.data

import android.content.Context
import android.content.SharedPreferences

class TimerRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    fun saveLastUsedTime(seconds: Int) {
        prefs.edit().putInt(KEY_LAST_USED_TIME, seconds).apply()
    }

    fun getLastUsedTime(): Int {
        return prefs.getInt(KEY_LAST_USED_TIME, 0)
    }

    fun saveLastMode(mode: Int) {
        prefs.edit().putInt(KEY_LAST_MODE, mode).apply()
    }

    fun getLastMode(): Int {
        return prefs.getInt(KEY_LAST_MODE, MODE_FIXED_RINGS)
    }

    companion object {
        private const val PREFS_NAME = "rainbow_timer_prefs"
        private const val KEY_LAST_USED_TIME = "last_used_time"
        private const val KEY_LAST_MODE = "last_mode"
        const val MODE_FIXED_RINGS = 1
        const val MODE_FIXED_RATE = 2
    }
}
