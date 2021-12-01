package dev.nmullaney.tesladashboard

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.WindowManager
import kotlin.math.absoluteValue

class LinearGauge @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val TAG = DashViewModel::class.java.simpleName
    private var windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var realDisplay = windowManager.defaultDisplay
    private var displayDimensions : Point = Point()
    private var screenWidth : Int = 100
    private var percentWidth : Float = 0f
    private var isSunUp : Int = 0
    private var lineColor : ColorFilter = PorterDuffColorFilter(getResources().getColor(R.color.dark_gray), PorterDuff.Mode.SRC_ATOP)
    private var renderWidth : Float = 100f

    fun getScreenWidth(): Int {
        return Resources.getSystem().displayMetrics.widthPixels
    }
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        realDisplay.getRealSize(displayDimensions)
        screenWidth = displayDimensions.x
        val paint = Paint()
        var startX : Float = 0f
        var stopX: Float = 0f
        paint.strokeWidth = 10f
        paint.setColorFilter(PorterDuffColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_ATOP))
        canvas?.drawLine(50f, 0f, screenWidth.toFloat() - 50f, 0f, paint)

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
        canvas?.drawLine(startX, 0f, stopX, 0f, paint)
        paint.setColorFilter(PorterDuffColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_ATOP))


    }
    fun setGauge(percent:Float){
        percentWidth = percent

        renderWidth = (screenWidth - 100f)/2f * (percent.absoluteValue)
        Log.d(TAG, "percentWidth $screenWidth RenderWidth $renderWidth")

    }
    fun setDayValue(isSunUpVal: Int = 1){
        isSunUp = isSunUpVal
        if (isSunUp == 1){
            lineColor = PorterDuffColorFilter(getResources().getColor(R.color.dark_gray), PorterDuff.Mode.SRC_ATOP)
        } else {
            lineColor = PorterDuffColorFilter(getResources().getColor(R.color.light_gray), PorterDuff.Mode.SRC_ATOP)

        }
    }
}