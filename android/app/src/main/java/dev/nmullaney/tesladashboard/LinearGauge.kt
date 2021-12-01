package dev.nmullaney.tesladashboard

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import kotlin.math.absoluteValue

class LinearGauge @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val TAG = DashViewModel::class.java.simpleName
    private val screenWidth : Int = Resources.getSystem().displayMetrics.widthPixels
    private var percentWidth : Float = 0f
    private var isSunUp : Int = 0
    private var lineColor : ColorFilter = PorterDuffColorFilter(getResources().getColor(R.color.dark_gray), PorterDuff.Mode.SRC_ATOP)
    private var renderWidth : Float = 100f
    fun getScreenWidth(): Int {
        return Resources.getSystem().getDisplayMetrics().widthPixels
    }
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val paint = Paint()
        paint.strokeWidth = 10f
        if (percentWidth < 0f){
            paint.setColorFilter(PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.SRC_ATOP))
        }else {
            paint.setColorFilter(lineColor)
        }
        Log.d(TAG, "ScreenWidth $screenWidth RenderWidth $renderWidth")
        canvas?.drawLine(0f, 0f, renderWidth, 0f, paint)


    }
    fun setGauge(percent:Float){
        percentWidth = percent

        renderWidth = screenWidth * (percent.absoluteValue)
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