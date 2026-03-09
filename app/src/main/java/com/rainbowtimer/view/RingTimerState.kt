package com.rainbowtimer.view

import android.graphics.Color

data class RingTimerState(
    val ringCount: Int,
    val activeRingIndex: Int,
    val sweepFraction: Float,
    val ringColors: List<Int>
) {
    companion object {
        // HSV rainbow: Red (0°) -> Orange -> Yellow -> Green -> Cyan -> Blue -> Violet (280°)
        // We go from outer (red) to inner (violet)
        private const val HUE_START = 0f      // Red
        private const val HUE_END = 280f     // Violet
        private const val SATURATION = 1f
        private const val VALUE = 1f

        fun generateColors(ringCount: Int): List<Int> {
            if (ringCount <= 0) return emptyList()
            
            // Generate smooth gradient from red to violet
            return (0 until ringCount).map { i ->
                val hue = if (ringCount == 1) {
                    HUE_START
                } else {
                    HUE_START + (HUE_END - HUE_START) * i / (ringCount - 1)
                }
                Color.HSVToColor(floatArrayOf(hue, SATURATION, VALUE))
            }
        }
    }
}
