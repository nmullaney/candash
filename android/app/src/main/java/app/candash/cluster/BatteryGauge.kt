package app.candash.cluster

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat

class BatteryGauge @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var isChargeMode : Boolean = false
    private var lineColor : ColorFilter
    private var backgroundColor : ColorFilter
    private var chargeColor : ColorFilter
    private var cyber : Boolean = false

    private var powerPercent : Float = 50f

    init {
        val typedValue = TypedValue()

        context.theme.resolveAttribute(R.attr.cyberMode, typedValue, true)
        cyber = typedValue.data != 0

        context.theme.resolveAttribute(R.attr.colorSecondary, typedValue, true)
        lineColor = PorterDuffColorFilter(typedValue.data, PorterDuff.Mode.SRC_ATOP)

        context.theme.resolveAttribute(R.attr.colorGhost, typedValue, true)
        backgroundColor = PorterDuffColorFilter(typedValue.data, PorterDuff.Mode.SRC_ATOP)

        chargeColor = PorterDuffColorFilter(resources.getColor(R.color.telltale_green), PorterDuff.Mode.SRC_ATOP)
    }

    override fun onDraw(canvas: Canvas?) {
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
        val deadBattery = ContextCompat.getDrawable(context, R.drawable.ic_deadbattery)
        deadBattery?.setBounds(0, 0, 50.px, 20.px)
        deadBattery?.draw(canvas)

        val powerWidth = powerPercent / 100 * 41f
        val paint = Paint()
        paint.strokeWidth = 15f.px
        paint.setColorFilter(if (isChargeMode) chargeColor else lineColor)
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
        paint.setColorFilter(backgroundColor)
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
        paint.setColorFilter(if (isChargeMode) chargeColor else lineColor)
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

    fun setChargeMode(isChargeModeVal: Boolean) {
        isChargeMode = isChargeModeVal
    }
}