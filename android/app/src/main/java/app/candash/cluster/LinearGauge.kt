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
    private val TAG = DashViewModel::class.java.simpleName
    private var windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var screenWidth : Int = 100
    private var percentWidth : Float = 0f
    private var isSunUp : Int = 0
    private var lineColor : ColorFilter = PorterDuffColorFilter(getResources().getColor(R.color.dark_gray), PorterDuff.Mode.SRC_ATOP)
    private var backgroundLineColor : ColorFilter = PorterDuffColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_ATOP)
    private var isSplitScreen = MutableLiveData<Boolean>()

    private var renderWidth : Float = 100f

    private var cyber : Boolean = false
    private var animationPosition : Float = 3f

    private var startupAnimator: ValueAnimator? = null

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

    // converts dp to px
    private fun Float.dpToPx(): Float {
        return this * resources.displayMetrics.density
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        screenWidth = getRealScreenWidth()
        // check if split screen
        if (isSplitScreen()){
            screenWidth = getScreenWidth()
        }
        val paint = Paint()
        var startX : Float = 0f
        var stopX: Float = 0f

        paint.strokeWidth = 6f.dpToPx()
        if (cyber) {
            paint.strokeCap = Paint.Cap.SQUARE
        } else {
            paint.strokeCap = Paint.Cap.ROUND
        }

        // Startup Animation Drawing:
        val bgAnimationPosition = if (animationPosition > 1f) 1f else animationPosition
        paint.setColorFilter(backgroundLineColor)
        val screenXCenter = screenWidth / 2f
        startX = screenXCenter - (screenXCenter - 20f.dpToPx()) * bgAnimationPosition
        stopX = screenXCenter + (screenXCenter - 20f.dpToPx()) * bgAnimationPosition
        canvas?.drawLine(startX, 3f.dpToPx(), stopX, 3f.dpToPx(), paint)

        if (animationPosition < 1f) {
            return
        } else if (animationPosition < 2f) {
            paint.setColorFilter(lineColor)
            // start at both ends and move to center (2 separate lines)
            val startXLeft = 20f.dpToPx()
            val stopXLeft = 20f.dpToPx() + (screenXCenter - 20f.dpToPx()) * (animationPosition - 1f)
            val startXRight = screenWidth - 20f.dpToPx()
            val stopXRight = screenWidth - 20f.dpToPx() - (screenXCenter - 20f.dpToPx()) * (animationPosition - 1f)
            canvas?.drawLine(startXLeft, 3f.dpToPx(), stopXLeft, 3f.dpToPx(), paint)
            canvas?.drawLine(startXRight, 3f.dpToPx(), stopXRight, 3f.dpToPx(), paint)
            return
        } else if (animationPosition < 3f) {
            paint.setColorFilter(lineColor)
            val invertedAnimationPosition = 3f - animationPosition
            // start at full line and shrink to center
            startX = screenXCenter - (screenXCenter - 20f.dpToPx()) * (invertedAnimationPosition)
            stopX = screenXCenter + (screenXCenter - 20f.dpToPx()) * (invertedAnimationPosition)
            canvas?.drawLine(startX, 3f.dpToPx(), stopX, 3f.dpToPx(), paint)
        }

        // Actual Gauge Drawing:
        if (percentWidth < 0f){
            paint.setColorFilter(PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.SRC_ATOP))
            startX = (screenWidth / 2f) - renderWidth
            stopX = screenWidth / 2f
        }else {
            startX = screenWidth / 2f
            stopX = (screenWidth / 2f) + renderWidth
            paint.setColorFilter(lineColor)
        }
        Log.d(TAG, "ScreenWidth $screenWidth RenderWidth $renderWidth")
        // Y needs to be strokeWidth/2 so the line doesn't get cut off.
        canvas?.drawLine(startX, 3f.dpToPx(), stopX, 3f.dpToPx(), paint)
        paint.setColorFilter(PorterDuffColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_ATOP))


    }
    fun setGauge(percent:Float){
        percentWidth = percent

        renderWidth = (screenWidth - 100f.dpToPx())/2f * (percent.absoluteValue)
        Log.d(TAG, "percentWidth $screenWidth RenderWidth $renderWidth")

        this.invalidate()
    }
    fun setDayValue(isSunUpVal: Int = 1){
        isSunUp = isSunUpVal
        if (isSunUp == 1){
            lineColor = PorterDuffColorFilter(getResources().getColor(R.color.dark_gray), PorterDuff.Mode.SRC_ATOP)
            backgroundLineColor = PorterDuffColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_ATOP)
        } else {
            lineColor = PorterDuffColorFilter(getResources().getColor(R.color.light_gray), PorterDuff.Mode.SRC_ATOP)
            backgroundLineColor = PorterDuffColorFilter(getResources().getColor(R.color.dark_gray), PorterDuff.Mode.SRC_ATOP)
        }
        this.invalidate()
    }

    fun setCyberMode(cyberMode:Boolean){
        if (!cyber && cyberMode) {
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
        cyber = cyberMode
        this.invalidate()
    }
}