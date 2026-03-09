package com.rainbowtimer.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.rainbowtimer.util.TimerConstants
import com.rainbowtimer.util.TimerLogger
import kotlin.math.min

class RingTimerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var state: RingTimerState? = null
    
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    
    private val gapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1A1A1A")
    }
    
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.BLACK
    }

    private val ringGap: Float
    private val centerRadius: Float
    
    private val rectF = RectF()

    init {
        val density = context.resources.displayMetrics.density
        ringGap = TimerConstants.GAP_BETWEEN_RINGS_DP * density
        centerRadius = TimerConstants.CENTER_CIRCLE_RADIUS_DP * density
    }

    fun setState(newState: RingTimerState) {
        TimerLogger.d("RingTimerView", "setState called - ringCount: ${newState.ringCount}, activeRing: ${newState.activeRingIndex}, sweepFraction: ${newState.sweepFraction}")
        state = newState
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        TimerLogger.d("RingTimerView", "onDraw called - width: $width, height: $height")
        
        val currentState = state ?: run {
            TimerLogger.d("RingTimerView", "state is null, returning")
            return
        }
        if (currentState.ringCount <= 0) {
            drawCenterCircle(canvas)
            return
        }

        val centerX = width / 2f
        val centerY = height / 2f
        
        TimerLogger.d("RingTimerView", "centerX: $centerX, centerY: $centerY")
        
        val maxRadius = min(centerX, centerY) - centerRadius - ringGap
        val ringWidth = (maxRadius - ringGap * (currentState.ringCount - 1)) / currentState.ringCount
        
        TimerLogger.d("RingTimerView", "maxRadius: $maxRadius, ringWidth: $ringWidth")
        
        if (ringWidth <= 0 || maxRadius <= 0) {
            TimerLogger.e("RingTimerView", "Invalid dimensions! ringWidth: $ringWidth, maxRadius: $maxRadius")
            drawCenterCircle(canvas)
            return
        }
        
        for (i in 0 until currentState.ringCount) {
            // i=0 is outermost ring, i=ringCount-1 is innermost
            val radius = centerRadius + ringGap + (ringWidth + ringGap) * (currentState.ringCount - 1 - i) + ringWidth / 2
            
            when {
                i < currentState.activeRingIndex -> {
                    // Outer rings that have already depleted - don't draw
                }
                i == currentState.activeRingIndex -> {
                    // Current ring being depleted - draw sweeping arc (counterclockwise from 12 o'clock)
                    ringPaint.color = currentState.ringColors.getOrElse(i) { Color.RED }
                    ringPaint.strokeWidth = ringWidth
                    ringPaint.alpha = 255
                    drawSweepingArcCounterclockwise(canvas, centerX, centerY, radius, currentState.sweepFraction)
                }
                else -> {
                    // Inner rings that haven't started yet - draw full circle
                    ringPaint.color = currentState.ringColors.getOrElse(i) { Color.RED }
                    ringPaint.strokeWidth = ringWidth
                    ringPaint.alpha = 255
                    drawFullCircle(canvas, centerX, centerY, radius, ringPaint)
                }
            }
        }
        
        drawCenterCircle(canvas)
    }

    private fun drawFullCircle(canvas: Canvas, cx: Float, cy: Float, radius: Float, paint: Paint) {
        rectF.set(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(rectF, 0f, 360f, false, paint)
    }

    private fun drawSweepingArcCounterclockwise(canvas: Canvas, cx: Float, cy: Float, radius: Float, fraction: Float) {
        // Draw arc remaining (counterclockwise from 12 o'clock)
        // fraction = 0 means full circle (360 degrees remaining)
        // fraction = 1 means empty (0 degrees remaining)
        val remainingAngle = 360f * (1f - fraction)
        rectF.set(cx - radius, cy - radius, cx + radius, cy + radius)
        // Start at 12 o'clock (-90 degrees), sweep counterclockwise (negative angle)
        canvas.drawArc(rectF, -90f, -remainingAngle, false, ringPaint)
    }

    private fun drawRingGaps(canvas: Canvas, cx: Float, cy: Float, radius: Float, ringWidth: Float) {
        val innerRadius = radius - ringWidth / 2 - ringGap / 2
        val outerRadius = radius + ringWidth / 2 + ringGap / 2
        
        canvas.drawCircle(cx, cy, innerRadius, gapPaint)
        canvas.drawCircle(cx, cy, outerRadius, gapPaint)
    }

    private fun drawCenterCircle(canvas: Canvas) {
        val centerX = width / 2f
        val centerY = height / 2f
        canvas.drawCircle(centerX, centerY, centerRadius, centerPaint)
    }
}
