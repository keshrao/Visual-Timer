package com.rainbowtimer.util

import android.util.Log

object TimerLogger {
    private var isDebugEnabled = true

    fun d(tag: String, message: String) {
        if (isDebugEnabled) {
            Log.d(tag, message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (isDebugEnabled) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }

    fun i(tag: String, message: String) {
        if (isDebugEnabled) {
            Log.i(tag, message)
        }
    }

    fun setDebugEnabled(enabled: Boolean) {
        isDebugEnabled = enabled
    }
}
