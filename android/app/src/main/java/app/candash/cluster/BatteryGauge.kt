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
    private var lightMode : Int = 1
    private var isChargeMode : Boolean = false
    private var lineColor : ColorFilter = PorterDuffColorFilter(resources.getColor(R.color.dark_gray), PorterDuff.Mode.SRC_ATOP)
    private var backgroundLineColor : ColorFilter = PorterDuffColorFilter(resources.getColor(R.color.battery_bg_day), PorterDuff.Mode.SRC_ATOP)

    private var powerPercent : Float = 50f

    private var cyber : Boolean = false

    override fun onDraw(canvas: Canvas?) {
        // check the value of the current theme's 'cyberMode' attribute to set 'cyber'
        val attrSet = intArrayOf(R.attr.cyberMode)
        val typedArray = context.obtainStyledAttributes(attrSet)
        cyber = typedArray.getBoolean(0, false)
        typedArray.recycle()

        super.onDraw(canvas)
        if (canvas == null) return

        if (cyber) {
            drawCyber(canvas)
        } else {
            drawClassic(canvas)
        }
    }

    private fun drawClassic(canvas: Canvas) {
        // first insert @drawable/ic_deadbattery
        val deadBattery = if (lightMode == 1) ContextCompat.getDrawable(context, R.drawable.ic_deadbattery) else ContextCompat.getDrawable(context, R.drawable.ic_deadbattery_night)
        deadBattery?.setBounds(0, 0, 50.px, 20.px)
        deadBattery?.draw(canvas)

        val powerWidth = powerPercent / 100 * 41f
        val paint = Paint()
        paint.strokeWidth = 15f.px
        paint.setColorFilter(lineColor)
        canvas.drawLine(3f.px, 10f.px, (3f+powerWidth).px, 10f.px, paint)
    }

    private fun drawCyber(canvas: Canvas) {
        val path = Path()
        val paint = Paint()
        paint.strokeWidth = 0f
        paint.style = Paint.Style.FILL

        // calculate position helpers
        val step = (width * 9 / 10 / 10).toFloat() // pitch from line to line, leaving room at the right
        val lineWidth = step / 2
        val tbMargin = 12f // top and bottom margin, because cyber is thinner than classic battery
        val top = 0f + tbMargin
        val bottom = height.toFloat() - tbMargin
        val fullLineCount = (powerPercent / 10).toInt()
        val remainder = powerPercent % 10
        val remHeight = remainder / 10 * (bottom - top)
        val remWidth = remainder / 10 * step * 2

        // draw background lines from fullLineCount to end
        paint.setColorFilter(backgroundLineColor)
        for (i in fullLineCount until 10) {
            path.reset()
            path.moveTo(i * step, bottom) // bottom left
            path.lineTo((i + 2) * step, top) // top left
            path.lineTo((i + 2) * step + lineWidth, top) // top right
            path.lineTo(i * step + lineWidth, bottom) // bottom right
            path.close()
            canvas.drawPath(path, paint)
        }
        // draw full lines, same as above, but only up to powerPercent
        paint.setColorFilter(lineColor)
        for (i in 0 until fullLineCount) {
            path.reset()
            path.moveTo(i * step, bottom) // bottom left
            path.lineTo((i + 2) * step, top) // top left
            path.lineTo((i + 2) * step + lineWidth, top) // top right
            path.lineTo(i * step + lineWidth, bottom) // bottom right
            path.close()
            canvas.drawPath(path, paint)
        }
        // draw the partial line
        if (remainder > 0) {
            path.reset()
            path.moveTo(fullLineCount * step, bottom) // bottom left
            path.lineTo(fullLineCount * step + remWidth, bottom - remHeight) // top left
            path.lineTo(fullLineCount * step + remWidth + lineWidth, bottom - remHeight) // top right
            path.lineTo(fullLineCount * step + lineWidth, bottom) // bottom right
            path.close()
            canvas.drawPath(path, paint)
        }
    }

    fun setGauge(percent:Float){
        powerPercent = percent

        this.invalidate()
    }

    private fun setColor() {
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
            PorterDuffColorFilter(resources.getColor(R.color.battery_bg_day), PorterDuff.Mode.SRC_ATOP)
        } else {
            PorterDuffColorFilter(resources.getColor(R.color.battery_bg_night), PorterDuff.Mode.SRC_ATOP)
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