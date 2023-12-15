package app.candash.cluster

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager

class CircularGauge @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val TAG = DashViewModel::class.java.simpleName
    private var windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var screenWidth : Int = 100
    private var percentWidth : Float = 0f
    private var lightMode : Int = 0
    private var lineColor : ColorFilter = PorterDuffColorFilter(getResources().getColor(R.color.dark_gray), PorterDuff.Mode.SRC_ATOP)
    private var backgroundLineColor : ColorFilter = PorterDuffColorFilter(getResources().getColor(R.color.medium_gray), PorterDuff.Mode.SRC_ATOP)

    private val hexAngle = 60.0
    private val paint = Paint()
    private val hexPath = Path()
    private val innerHexPath = Path()
    private val outerHexPath = Path()

    private var powerWidth : Float = 0f
    private var strokeWidthPct : Float = 12f
    private var charging : Boolean = false

    private var cyber : Boolean = false

    private fun buildHexPaths() {
        val radius = Math.min(width, height) / 2f - paint.strokeWidth / 2
        for (i in 0 until 6) {
            val x = width / 2 + radius * Math.cos(Math.toRadians((i * hexAngle))).toFloat()
            val y = height / 2 + radius * Math.sin(Math.toRadians((i * hexAngle))).toFloat()
            if (i == 0) {
                hexPath.moveTo(x, y)
            } else {
                hexPath.lineTo(x, y)
            }
        }
        hexPath.close()

        val innerRadius = Math.min(width, height) / 2f - paint.strokeWidth
        for (i in 0 until 6) {
            val x = width / 2 + innerRadius * Math.cos(Math.toRadians((i * hexAngle))).toFloat()
            val y = height / 2 + innerRadius * Math.sin(Math.toRadians((i * hexAngle))).toFloat()
            if (i == 0) {
                innerHexPath.moveTo(x, y)
            } else {
                innerHexPath.lineTo(x, y)
            }
        }
        innerHexPath.close()

        val outerRadius = Math.min(width, height) / 2f
        for (i in 0 until 6) {
            val x = width / 2 + outerRadius * Math.cos(Math.toRadians((i * hexAngle))).toFloat()
            val y = height / 2 + outerRadius * Math.sin(Math.toRadians((i * hexAngle))).toFloat()
            if (i == 0) {
                outerHexPath.moveTo(x, y)
            } else {
                outerHexPath.lineTo(x, y)
            }
        }
        outerHexPath.close()
    }
    fun getScreenWidth(): Int {
        var displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    fun getRealScreenWidth(): Int {
        var displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    private fun isSplitScreen(): Boolean {
        return getRealScreenWidth() > getScreenWidth() * 2
    }



    override fun onDraw(canvas: Canvas?) {
        Log.d(TAG, "width $width height $height")

        super.onDraw(canvas)
        screenWidth = getRealScreenWidth()
        // check if split screen
        if (isSplitScreen()){
            screenWidth = getScreenWidth()
        }

        paint.strokeWidth = strokeWidthPct / 100f * width
        paint.style = Paint.Style.STROKE
        paint.setColorFilter(backgroundLineColor)

        if (cyber) {
            buildHexPaths()
            canvas?.drawPath(hexPath, paint)
        } else {
            canvas?.drawArc(
                0f + paint.strokeWidth / 2,
                0f + paint.strokeWidth / 2,
                width - paint.strokeWidth / 2,
                height - paint.strokeWidth / 2,
                90f,
                360f,
                false,
                paint
            )
        }

        if (powerWidth < 0f) {
            paint.colorFilter = PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.SRC_ATOP)
        } else {
            paint.colorFilter = lineColor
        }
        if (charging){
            paint.colorFilter = PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.SRC_ATOP)
        }
        if (cyber) {
            paint.strokeCap = Paint.Cap.BUTT
        } else {
            paint.strokeCap = Paint.Cap.ROUND
        }
        if (powerWidth != 0f) {
            if (cyber) {
                // We're cheating a bit and drawing an arc under a hex mask
                canvas?.save()
                canvas?.clipPath(outerHexPath)
                canvas?.clipPath(innerHexPath, Region.Op.DIFFERENCE)
                paint.strokeWidth = paint.strokeWidth * 3
                canvas?.drawArc(
                    0f + paint.strokeWidth / 2,
                    0f + paint.strokeWidth / 2,
                    width - paint.strokeWidth / 2,
                    height - paint.strokeWidth / 2,
                    90f,
                    powerWidth,
                    false,
                    paint
                )
                canvas?.restore()
            } else {
                canvas?.drawArc(
                    0f + paint.strokeWidth / 2,
                    0f + paint.strokeWidth / 2,
                    width - paint.strokeWidth / 2,
                    height - paint.strokeWidth / 2,
                    90f,
                    powerWidth,
                    false,
                    paint
                )
            }
        }
    }
    fun setGauge(percent:Float, sWidthPct:Float = strokeWidthPct, charge:Boolean = false){
        powerWidth = percent * 360f
        charging = charge
        strokeWidthPct = sWidthPct

        this.invalidate()
    }


    fun setDayValue(lightModeVal: Int = 1){
        lightMode = lightModeVal
        if (lightMode == 1){
            lineColor = PorterDuffColorFilter(getResources().getColor(R.color.black), PorterDuff.Mode.SRC_ATOP)
            backgroundLineColor = PorterDuffColorFilter(getResources().getColor(R.color.medium_gray), PorterDuff.Mode.SRC_ATOP)
        } else {
            lineColor = PorterDuffColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP)
            backgroundLineColor = PorterDuffColorFilter(getResources().getColor(R.color.dark_gray), PorterDuff.Mode.SRC_ATOP)
        }
        this.invalidate()
    }

    fun setCyberMode(cyberMode: Boolean = false){
        cyber = cyberMode
        this.invalidate()
    }
}