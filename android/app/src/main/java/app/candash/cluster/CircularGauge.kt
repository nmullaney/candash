package app.candash.cluster

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import kotlin.math.cos
import kotlin.math.sin

class CircularGauge @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var lineColor : ColorFilter
    private var chargeColor : ColorFilter
    private var backgroundColor : ColorFilter
    private var cyber : Boolean = false

    private val hexAngle = 60.0
    private val paint = Paint()
    private val hexPath = Path()
    private val innerHexPath = Path()
    private val outerHexPath = Path()
    private val fullCirclePath = Path()
    private var pathMeasure: PathMeasure? = null
    private val partialCirclePath = Path()

    private var powerWidth : Float = 0f
    private var strokeWidthPct : Float = 12f
    private var charging : Boolean = false

    init {
        val typedValue = TypedValue()

        context.theme.resolveAttribute(R.attr.cyberMode, typedValue, true)
        cyber = typedValue.data != 0

        context.theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
        lineColor = PorterDuffColorFilter(typedValue.data, PorterDuff.Mode.SRC_ATOP)

        context.theme.resolveAttribute(R.attr.colorGhost, typedValue, true)
        backgroundColor = PorterDuffColorFilter(typedValue.data, PorterDuff.Mode.SRC_ATOP)

        chargeColor = PorterDuffColorFilter(resources.getColor(R.color.telltale_green), PorterDuff.Mode.SRC_ATOP)

        paint.apply {
            style = Paint.Style.STROKE
            strokeCap = if (cyber) Paint.Cap.BUTT else Paint.Cap.ROUND
        }
    }

    private fun buildFullCirclePath() {
        fullCirclePath.reset() // Clear any previous paths.
        // Define the bounds for the full circle
        val diameter = width.coerceAtMost(height) - paint.strokeWidth
        val left = paint.strokeWidth / 2
        val top = paint.strokeWidth / 2
        val right = left + diameter
        val bottom = top + diameter
        // Create a full circle path starting from the bottom (6 o'clock)
        fullCirclePath.addArc(left, top, right, bottom, 90f, 360f)
        pathMeasure = PathMeasure(fullCirclePath, true)
    }
    private fun buildHexPaths() {
        hexPath.reset()
        val radius = width.coerceAtMost(height) / 2f - paint.strokeWidth / 2
        for (i in 0 until 6) {
            val x = width / 2 + radius * cos(Math.toRadians((i * hexAngle))).toFloat()
            val y = height / 2 + radius * sin(Math.toRadians((i * hexAngle))).toFloat()
            if (i == 0) {
                hexPath.moveTo(x, y)
            } else {
                hexPath.lineTo(x, y)
            }
        }
        hexPath.close()

        innerHexPath.reset()
        val innerRadius = width.coerceAtMost(height) / 2f - paint.strokeWidth
        for (i in 0 until 6) {
            val x = width / 2 + innerRadius * cos(Math.toRadians((i * hexAngle))).toFloat()
            val y = height / 2 + innerRadius * sin(Math.toRadians((i * hexAngle))).toFloat()
            if (i == 0) {
                innerHexPath.moveTo(x, y)
            } else {
                innerHexPath.lineTo(x, y)
            }
        }
        innerHexPath.close()

        outerHexPath.reset()
        val outerRadius = width.coerceAtMost(height) / 2f
        for (i in 0 until 6) {
            val x = width / 2 + outerRadius * cos(Math.toRadians((i * hexAngle))).toFloat()
            val y = height / 2 + outerRadius * sin(Math.toRadians((i * hexAngle))).toFloat()
            if (i == 0) {
                outerHexPath.moveTo(x, y)
            } else {
                outerHexPath.lineTo(x, y)
            }
        }
        outerHexPath.close()
    }

    private fun setStrokeWidth() {
        paint.strokeWidth = strokeWidthPct / 100f * width
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setStrokeWidth()
        buildFullCirclePath()
        buildHexPaths()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        setStrokeWidth()
        paint.colorFilter = backgroundColor

        if (cyber) {
            canvas?.drawPath(hexPath, paint)
        } else {
            canvas?.drawPath(fullCirclePath, paint)
        }

        paint.colorFilter = when {
            charging -> chargeColor
            powerWidth < 0f -> chargeColor
            else -> lineColor
        }
        if (powerWidth != 0f) {
            if (cyber) {
                // We're cheating a bit and drawing the arc path under a hex mask
                canvas?.save()
                canvas?.clipPath(outerHexPath)
                canvas?.clipPath(innerHexPath, Region.Op.DIFFERENCE)
                paint.strokeWidth *= 5 // this will be reset in setStrokeWidth on next draw
            }

            // Calculate the length of the path that corresponds to the current powerWidth.
            val length = pathMeasure?.length ?: 0f
            val stop = length * (powerWidth / 360f)
            partialCirclePath.reset()
            pathMeasure?.getSegment(0f, stop, partialCirclePath, true)
            canvas?.drawPath(partialCirclePath, paint)

            if (cyber) {
                canvas?.restore()
            }
        }
    }
    fun setGauge(percent:Float, sWidthPct:Float = strokeWidthPct, charge:Boolean = false){
        powerWidth = percent * 360f
        charging = charge
        if (strokeWidthPct != sWidthPct) {
            strokeWidthPct = sWidthPct
            setStrokeWidth()
            buildFullCirclePath()
            buildHexPaths()
        }

        this.invalidate()
    }
}