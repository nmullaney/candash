package app.candash.cluster

import android.content.res.Resources
import kotlin.math.abs


val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Float.px: Float
    get() = (this * Resources.getSystem().displayMetrics.density)

/**
 * This rounds a float to the provided decimal places and returns a string
 * which is formatted to show that many decimal places.
 *
 * @param dp If null, decimal places are automatically determined by size of the Float
 */
fun Float.roundToString(dp: Int? = null): String {
    return when {
        dp != null -> "%.${dp}f".format(this)
        abs(this) < 1f -> "%.2f".format(this)
        abs(this) < 10f -> "%.1f".format(this)
        else -> "%.0f".format(this)
    }
}