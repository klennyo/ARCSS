package com.surendramaran.yolov8tflite

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.res.ResourcesCompat
import android.util.AttributeSet
import android.view.View

class BoundingBoxOverlay(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var boxes: List<BoundingBox> = emptyList()

    private val defaultPaint = Paint().apply {
        color = Color.MAGENTA
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        style = Paint.Style.FILL
        typeface = ResourcesCompat.getFont(context, R.font.pop)
    }

    fun setBoundingBoxes(boundingBoxes: List<BoundingBox>) {
        boxes = boundingBoxes
        invalidate() // Force redraw
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        boxes.forEach { box ->
            val scaledWidth = box.w * box.scaleWidth * width
            val scaledHeight = box.h * box.scaleHeight * height
            val left = box.x1 * width
            val top = box.y1 * height
            val right = left + scaledWidth
            val bottom = top + scaledHeight

            val scaledRect = RectF(left, top, right, bottom)
            val boxPaint = getBoxPaint(box) // Uses box.cls to determine color
            canvas.drawRect(scaledRect, boxPaint)
            canvas.drawText(box.clsName, scaledRect.left, scaledRect.top - textPaint.textSize, textPaint)
        }
    }

    private fun getBoxPaint(box: BoundingBox): Paint = Paint().apply {
        color = when (box.clsName) {
            "CPU socket (MISSING_1)",
            "CPU fan interface (DETECTED)",
            "Power Interface (DETECTED)",
            "CPU power interface (DETECTED)",
            "INCOMPLETE"-> Color.RED
            else -> defaultPaint.color
        }
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
}


data class BoundingBox(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val cx: Float,
    val cy: Float,
    val w: Float,
    val h: Float,
    val cnf: Float,
    val cls: Int,
    val clsName: String,
    val scaleWidth: Float = 1.0f, // Default scale for width
    val scaleHeight: Float = 1.0f // Default scale for height
)

