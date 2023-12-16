package app.candash.cluster

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.lifecycle.MutableLiveData
import kotlin.math.absoluteValue

class LinearGauge @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var percentWidth : Float = 0.5f
    private var lightMode : Int = 0
    private var lineColor : ColorFilter = PorterDuffColorFilter(resources.getColor(R.color.dark_gray), PorterDuff.Mode.SRC_ATOP)
    private val greenColor : ColorFilter = PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.SRC_ATOP)
    private var backgroundLineColor : ColorFilter = PorterDuffColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_ATOP)
    private val paint = Paint()

    private var cyber : Boolean = false
    private var animationPosition : Float = 3f

    private var startupAnimator: ValueAnimator? = null

    override fun onDraw(canvas: Canvas?) {
        // check the value of the current theme's 'cyberMode' attribute to set 'cyber'
        val attrSet = intArrayOf(R.attr.cyberMode)
        val typedArray = context.obtainStyledAttributes(attrSet)
        val newCyber = typedArray.getBoolean(0, false)
        typedArray.recycle()

        if (!cyber && newCyber) {
            startupAnimator = ValueAnimator.ofFloat(0f, 3f).apply {
                duration = 2000L
                addUpdateListener { animator ->
                    val fraction = animator.animatedValue as Float
                    animationPosition = fraction
                    invalidate()
                }
                start()
            }
        }
        cyber = newCyber

        super.onDraw(canvas)
        var startX: Float
        var stopX: Float

        paint.strokeWidth = 6f.px
        if (cyber) {
            paint.strokeCap = Paint.Cap.SQUARE
        } else {
            paint.strokeCap = Paint.Cap.ROUND
        }

        // Startup Animation Drawing:
        val bgAnimationPosition = if (animationPosition > 1f) 1f else animationPosition
        paint.setColorFilter(backgroundLineColor)
        val halfX = width / 2f
        startX = halfX - halfX * bgAnimationPosition
        stopX = halfX + halfX * bgAnimationPosition
        canvas?.drawLine(startX, 3f.px, stopX, 3f.px, paint)

        if (animationPosition < 1f) {
            return
        } else if (animationPosition < 2f) {
            paint.setColorFilter(lineColor)
            // start at both ends and move to center (2 separate lines)
            val startXLeft = 0f
            val stopXLeft = halfX * (animationPosition - 1f)
            val startXRight = width.toFloat()
            val stopXRight = width - halfX * (animationPosition - 1f)
            canvas?.drawLine(startXLeft, 3f.px, stopXLeft, 3f.px, paint)
            canvas?.drawLine(startXRight, 3f.px, stopXRight, 3f.px, paint)
            return
        } else if (animationPosition < 3f) {
            paint.setColorFilter(lineColor)
            val invertedAnimationPosition = 3f - animationPosition
            // start at full line and shrink to center
            startX = halfX - (halfX * invertedAnimationPosition)
            stopX = halfX + (halfX * invertedAnimationPosition)
            canvas?.drawLine(startX, 3f.px, stopX, 3f.px, paint)
        }

        // Actual Gauge Drawing:
        val renderWidth = width / 2f * (percentWidth.absoluteValue)
        if (percentWidth < 0f){
            paint.setColorFilter(greenColor)
            startX = (width / 2f) - renderWidth
            stopX = width / 2f
        }else {
            startX = width / 2f
            stopX = (width / 2f) + renderWidth
            paint.setColorFilter(lineColor)
        }
        // Y needs to be strokeWidth/2 so the line doesn't get cut off.
        canvas?.drawLine(startX, 3f.px, stopX, 3f.px, paint)
    }
    fun setGauge(percent:Float){
        percentWidth = percent

        this.invalidate()
    }
    fun setDayValue(lightModeVal: Int = 1){
        lightMode = lightModeVal
        if (lightMode == 1){
            lineColor = PorterDuffColorFilter(resources.getColor(R.color.dark_gray), PorterDuff.Mode.SRC_ATOP)
            backgroundLineColor = PorterDuffColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_ATOP)
        } else {
            lineColor = PorterDuffColorFilter(resources.getColor(R.color.light_gray), PorterDuff.Mode.SRC_ATOP)
            backgroundLineColor = PorterDuffColorFilter(resources.getColor(R.color.dark_gray), PorterDuff.Mode.SRC_ATOP)
        }
        this.invalidate()
    }
}