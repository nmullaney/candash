package app.candash.cluster

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager

class BatteryGauge @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    val Int.dp: Int
        get() = (this / Resources.getSystem().displayMetrics.density).toInt()
    val Int.px: Int
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()

    val Float.dp: Float
        get() = (this / Resources.getSystem().displayMetrics.density)
    val Float.px: Float
        get() = (this * Resources.getSystem().displayMetrics.density)
    private val TAG = DashViewModel::class.java.simpleName
    private var windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var screenWidth : Int = 100
    private var percentWidth : Float = 0f
    private var isSunUp : Int = 0
    private var lineColor : ColorFilter = PorterDuffColorFilter(getResources().getColor(R.color.dark_gray), PorterDuff.Mode.SRC_ATOP)
    private var backgroundLineColor : ColorFilter = PorterDuffColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_ATOP)

    private var powerWidth : Float = 0f

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
        super.onDraw(canvas)
        screenWidth = getRealScreenWidth()
        // check if split screen
        if (isSplitScreen()){
            screenWidth = getScreenWidth()
        }
        val paint = Paint()
        var startX : Float = 0f
        var stopX: Float = 0f
        paint.strokeWidth = 15f.px
        paint.setColorFilter(lineColor)
        canvas?.drawLine(3f.px, 10f.px, (3f+powerWidth).px, 10f.px, paint)


    }
    fun setGauge(percent:Float){
        powerWidth = percent/100 * 41f

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