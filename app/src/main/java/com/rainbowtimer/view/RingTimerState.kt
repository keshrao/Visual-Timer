package com.rainbowtimer.view

import android.graphics.Color

data class RingTimerState(
    val ringCount: Int,
    val activeRingIndex: Int,
    val sweepFraction: Float,
    val ringColors: List<Int>
) {
    companion object {
        val RAINBOW_COLORS = listOf(
            Color.parseColor("#FF0000"),
            Color.parseColor("#FF4500"),
            Color.parseColor("#FF8C00"),
            Color.parseColor("#FFD700"),
            Color.parseColor("#9ACD32"),
            Color.parseColor("#32CD32"),
            Color.parseColor("#00FF00"),
            Color.parseColor("#00FA9A"),
            Color.parseColor("#00CED1"),
            Color.parseColor("#00BFFF"),
            Color.parseColor("#1E90FF"),
            Color.parseColor("#0000FF"),
            Color.parseColor("#8A2BE2"),
            Color.parseColor("#9932CC"),
            Color.parseColor("#BA55D3"),
            Color.parseColor("#DDA0DD"),
            Color.parseColor("#FF1493"),
            Color.parseColor("#FF69B4"),
            Color.parseColor("#FFC0CB")
        )

        fun generateColors(ringCount: Int): List<Int> {
            if (ringCount <= 0) return emptyList()
            if (ringCount >= RAINBOW_COLORS.size) return RAINBOW_COLORS
            
            val step = (RAINBOW_COLORS.size - 1).toFloat() / (ringCount - 1)
            return (0 until ringCount).map { i ->
                val index = (i * step).toInt().coerceIn(0, RAINBOW_COLORS.size - 1)
                RAINBOW_COLORS[index]
            }
        }
    }
}
