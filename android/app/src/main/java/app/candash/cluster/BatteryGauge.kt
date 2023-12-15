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

    private var powerWidth : Float = 20f

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
        if (canvas == null) return
        // first insert @drawable/ic_deadbattery
        val deadBattery = ContextCompat.getDrawable(context, R.drawable.ic_deadbattery)
        deadBattery?.setBounds(0, 0, 50.px, 20.px)
        deadBattery?.draw(canvas)

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
        canvas.drawLine(3f.px, 10f.px, (3f+powerWidth).px, 10f.px, paint)


    }
    fun setGauge(percent:Float){
        powerWidth = percent/100 * 41f

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