package app.candash.cluster

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat

class BatteryGauge @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val TAG = DashViewModel::class.java.simpleName
    private var windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var screenWidth : Int = 100
    private var percentWidth : Float = 0f
    private var lightMode : Int = 0
    private var isChargeMode : Boolean = false
    private var lineColor : ColorFilter = PorterDuffColorFilter(getResources().getColor(R.color.dark_gray), PorterDuff.Mode.SRC_ATOP)
    private var backgroundLineColor : ColorFilter = PorterDuffColorFilter(Color.parseColor("#FFAAAAAA"), PorterDuff.Mode.SRC_ATOP)

    private var powerPercent : Float = 50f

    private var cyber : Boolean = false

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
        // check the value of the current theme's 'cyberMode' attribute to set 'cyber'
        val attrSet = intArrayOf(R.attr.cyberMode)
        val typedArray = context.obtainStyledAttributes(attrSet)
        cyber = typedArray.getBoolean(0, false)
        typedArray.recycle()

        super.onDraw(canvas)
        if (canvas == null) return
        screenWidth = getRealScreenWidth()
        // check if split screen
        if (isSplitScreen()){
            screenWidth = getScreenWidth()
        }

        if (cyber) {
            drawCyber(canvas)
        } else {
            drawClassic(canvas)
        }
    }

    private fun drawClassic(canvas: Canvas) {
        // first insert @drawable/ic_deadbattery
        val deadBattery = ContextCompat.getDrawable(context, R.drawable.ic_deadbattery)
        deadBattery?.setBounds(0, 0, 50.px, 20.px)
        deadBattery?.draw(canvas)

        val powerWidth = powerPercent / 100 * 41f
        val paint = Paint()
        var startX : Float = 0f
        var stopX: Float = 0f
        paint.strokeWidth = 15f.px
        paint.setColorFilter(lineColor)
        canvas.drawLine(3f.px, 10f.px, (3f+powerWidth).px, 10f.px, paint)
    }

    private fun drawCyber(canvas: Canvas) {
        // start by drawing 10 diagonal background lines
        val paint = Paint()
        paint.setColorFilter(backgroundLineColor)
        paint.strokeWidth = 2f.px
        paint.strokeCap = Paint.Cap.SQUARE

        // Clip is to keep the ends of the lines parallel to the edge of the screen
        val clipMargin = 10f
        val rectClipPath = Path()
        rectClipPath.addRect(0f, clipMargin, width.toFloat(), height.toFloat() - clipMargin, Path.Direction.CW)
        canvas.clipPath(rectClipPath)

        val step = (width * 9 / 10 / 10).toFloat() // each step is 10% of 90% of the width, because each line is step*2
        val xOffset = 1f.px
        for (i in 0..9) {
            canvas.drawLine(
                i * step + xOffset,
                height.toFloat() - clipMargin,
                (i + 2) * step + xOffset,
                clipMargin,
                paint
            )
        }

        paint.setColorFilter(lineColor)
        // draw diagonal power lines, same as above, but only up to powerPercent
        val fullLineCount = (powerPercent / 10).toInt()
        for (i in 0..fullLineCount - 1) {
            canvas.drawLine(
                i * step + xOffset,
                height.toFloat() - clipMargin,
                (i + 2) * step + xOffset,
                clipMargin,
                paint
            )
        }
        // draw the remainder
        paint.strokeCap = Paint.Cap.BUTT
        val remainder = powerPercent % 10
        val drawHeight = height.toFloat() - clipMargin * 2
        val remHeight = drawHeight * remainder / 10
        val remWidth = remainder / 10 * step * 2
        if (remainder > 0) {
            canvas.drawLine(
                fullLineCount * step + xOffset,
                height.toFloat() - clipMargin,
                fullLineCount * step + xOffset + remWidth,
                clipMargin + drawHeight - remHeight,
                paint
            )
        }
    }

    fun setGauge(percent:Float){
        powerPercent = percent

        this.invalidate()
    }

    fun setColor() {
        lineColor = if (isChargeMode) {
            PorterDuffColorFilter(resources.getColor(R.color.telltale_green), PorterDuff.Mode.SRC_ATOP)
        } else {
            if (lightMode == 1){
                PorterDuffColorFilter(resources.getColor(R.color.dark_gray), PorterDuff.Mode.SRC_ATOP)
            } else {
                PorterDuffColorFilter(resources.getColor(R.color.light_gray), PorterDuff.Mode.SRC_ATOP)
            }
        }
        backgroundLineColor = if (lightMode == 1){
            PorterDuffColorFilter(Color.parseColor("#FFAAAAAA"), PorterDuff.Mode.SRC_ATOP)
        } else {
            PorterDuffColorFilter(Color.DKGRAY, PorterDuff.Mode.SRC_ATOP)
        }
        this.invalidate()
    }

    fun setDayValue(lightModeVal: Int = 1){
        lightMode = lightModeVal
        setColor()
    }

    fun setChargeMode(isChargeModeVal: Boolean) {
        isChargeMode = isChargeModeVal
        setColor()
    }
}