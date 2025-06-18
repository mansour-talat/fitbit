package com.su.gym

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

class ProgressChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dataPoints = mutableListOf<Float>()
    private val linePaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val fillPaint = Paint().apply {
        color = Color.argb(50, 0, 0, 255)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 30f
        isAntiAlias = true
    }

    fun setData(data: List<Float>) {
        dataPoints.clear()
        dataPoints.addAll(data)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val padding = 50f
        val chartWidth = width - 2 * padding
        val chartHeight = height - 2 * padding

        // Find min and max values
        val maxValue = dataPoints.maxOrNull() ?: 0f
        val minValue = dataPoints.minOrNull() ?: 0f
        val valueRange = max(1f, maxValue - minValue)

        // Draw the line
        val path = Path()
        val fillPath = Path()

        dataPoints.forEachIndexed { index, value ->
            val x = padding + (index.toFloat() / (dataPoints.size - 1)) * chartWidth
            val y = height - padding - ((value - minValue) / valueRange) * chartHeight

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height - padding)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        // Close the fill path
        fillPath.lineTo(width - padding, height - padding)
        fillPath.close()

        // Draw the filled area
        canvas.drawPath(fillPath, fillPaint)
        // Draw the line
        canvas.drawPath(path, linePaint)

        // Draw min and max values
        canvas.drawText("${maxValue.toInt()}", padding, padding + textPaint.textSize, textPaint)
        canvas.drawText("${minValue.toInt()}", padding, height - padding, textPaint)
    }
} 