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
    private var backgroundLineColor : ColorFilter = PorterDuffColorFilter(getResources().getColor(R.color.medium_gray), PorterDuff.Mode.SRC_ATOP)

    private var powerWidth : Float = 0f
    private var strokeWidth : Float = 12f
    private var charging : Boolean = false


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
        val width = canvas!!.width
        val height = canvas!!.height
        val radius = Math.max(width, height)/2
        Log.d(TAG, "width $width height $height")

        var rect  = RectF(0f.px, height.toFloat(), width.toFloat(), 0f.px)
        super.onDraw(canvas)
        screenWidth = getRealScreenWidth()
        // check if split screen
        if (isSplitScreen()){
            screenWidth = getScreenWidth()
        }
        val paint = Paint()
        var startX : Float = 0f
        var stopX: Float = 0f
        paint.strokeWidth = strokeWidth
        paint.style = Paint.Style.STROKE
        rect.inset(0f,-paint.strokeWidth/2);
        paint.setColorFilter(backgroundLineColor)
        canvas?.drawArc(0f+paint.strokeWidth/2,  0f + paint.strokeWidth/2, width.toFloat() - paint.strokeWidth/2, height.toFloat() - paint.strokeWidth/2 , 90f, 360f, false, paint)

        if (powerWidth < 0f) {
            paint.colorFilter = PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.SRC_ATOP)
        } else {
            paint.colorFilter = lineColor
        }
        if (charging){
            paint.colorFilter = PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.SRC_ATOP)
            paint.strokeCap = Paint.Cap.ROUND
        }
        if (powerWidth != 0f) {
            canvas?.drawArc(
                0f + paint.strokeWidth / 2,
                0f + paint.strokeWidth / 2,
                width.toFloat() - paint.strokeWidth / 2,
                height.toFloat() - paint.strokeWidth / 2,
                90f,
                powerWidth,
                false,
                paint
            )
        }
        //canvas?.drawArc(rect, 0f, 360f, true, paint)


    }
    fun setGauge(percent:Float, sWidth:Float = 8f, charge:Boolean = false){
        powerWidth = percent * 360f
        charging = charge
        strokeWidth = sWidth

    }


    fun setDayValue(isSunUpVal: Int = 1){
        isSunUp = isSunUpVal
        if (isSunUp == 1){
            lineColor = PorterDuffColorFilter(getResources().getColor(R.color.black), PorterDuff.Mode.SRC_ATOP)
            backgroundLineColor = PorterDuffColorFilter(getResources().getColor(R.color.medium_gray), PorterDuff.Mode.SRC_ATOP)
        } else {
            lineColor = PorterDuffColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP)
            backgroundLineColor = PorterDuffColorFilter(getResources().getColor(R.color.dark_gray), PorterDuff.Mode.SRC_ATOP)


        }
    }
}