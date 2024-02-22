package app.candash.cluster

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import java.lang.Float.max
import java.lang.Float.min
import kotlin.math.round

class EfficiencyChart @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var minKm = -8f
    private val maxKm = 0f
    private val minEfficiency = -200f
    private val maxEfficiency = 600f
    private var odoEfficiencyPairs = createRandomData()
    private val paint = Paint()

    // Transparency is added in onDraw
    private var positiveEndColor: Int
    private var positiveStartColor: Int
    private val negativeEndColor = resources.getColor(R.color.telltale_green)
    // negative start color is slightly transparent version of negative end color
    private val negativeStartColor = Color.argb(
        (Color.alpha(negativeEndColor) * 0.3).toInt(),
        Color.red(negativeEndColor),
        Color.green(negativeEndColor),
        Color.blue(negativeEndColor)
    )

    init {
        val typedValue = TypedValue()

        context.theme.resolveAttribute(R.attr.colorSecondary, typedValue, true)
        positiveEndColor = typedValue.data

        context.theme.resolveAttribute(R.attr.colorGhost, typedValue, true)
        positiveStartColor = typedValue.data
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null) return

        this.alpha = 0.3f
        drawLineChart(canvas, odoEfficiencyToPointF(odoEfficiencyPairs))
    }

    fun setLookBack(sizeKm: Float) {
        minKm = -sizeKm
    }

    private fun odoEfficiencyToPointF(input: List<Pair<Float, Float>>): List<PointF> {
        val converted = mutableListOf<PointF>()
        for ((kmAgo, efficiency) in input) {
            // Round values to whole pixels
            converted.add(PointF(round(kmAgoToX(kmAgo)), round(efficiencyToY(efficiency))))
        }
        return converted.toList()
    }

    private fun drawLineChart(canvas: Canvas, points: List<PointF>) {
        if (points.isEmpty()) return

        val zeroY = efficiencyToY(0f)
        val firstX = points[0].x
        val fullPositiveColor = efficiencyToY(200f)

        val negativeShader =
            LinearGradient(
                0f,
                zeroY,
                0f,
                height.toFloat(),
                negativeStartColor,
                negativeEndColor,
                Shader.TileMode.CLAMP
            )
        val positiveShader =
            LinearGradient(
                0f,
                zeroY,
                0f,
                fullPositiveColor,
                positiveStartColor,
                positiveEndColor,
                Shader.TileMode.CLAMP
            )
        paint.style = Paint.Style.FILL

        // Set starting point of paths
        val negativePath = Path()
        negativePath.moveTo(firstX, zeroY)
        val positivePath = Path()
        positivePath.moveTo(firstX, zeroY)

        // Draw each path segment to the next point
        val lastIndex = points.size - 1
        for (i in points.indices) {
            // Clamp last point.x to screen width
            val point = if (i == lastIndex)
                PointF(width.toFloat(), points[i].y)
            else points[i]

            // Clamp last nextPoint.x to screen width
            val nextPoint = if (i + 1 == lastIndex)
                PointF(width.toFloat(), points[i + 1].y)
            else if (i < lastIndex)
                points[i + 1]
            else null

            if (point.y >= zeroY) {
                negativePath.lineTo(point.x, point.y)
                positivePath.lineTo(point.x, zeroY)
            } else {
                negativePath.lineTo(point.x, zeroY)
                positivePath.lineTo(point.x, point.y)
            }

            // Check for intersection with zero Y-axis in the next segment
            if (nextPoint != null && (point.y - zeroY) * (nextPoint.y - zeroY) < 0) {
                // There is a zero crossing, find the intersection X
                val intersectionX = findIntersectionX(point, nextPoint, zeroY)

                // Draw lines to intersection X, to be completed in the next iteration
                if (point.y >= zeroY) {
                    negativePath.lineTo(intersectionX, zeroY)
                    positivePath.lineTo(intersectionX, zeroY)
                } else {
                    positivePath.lineTo(intersectionX, zeroY)
                    negativePath.lineTo(intersectionX, zeroY)
                }
            }
        }

        // Draw lines back to zero y and close to left
        negativePath.lineTo(width.toFloat(), zeroY)
        positivePath.lineTo(width.toFloat(), zeroY)
        negativePath.close()
        positivePath.close()

        // Draw paths filled by shaders
        paint.shader = negativeShader
        canvas.drawPath(negativePath, paint)
        paint.shader = positiveShader
        canvas.drawPath(positivePath, paint)
    }

    private fun findIntersectionX(p1: PointF, p2: PointF, zeroY: Float): Float {
        val slope = (p2.y - p1.y) / (p2.x - p1.x)
        return p1.x + (zeroY - p1.y) / slope
    }

    /**
     * Creates random efficiency data for testing how it looks in the fragment preview
     */
    private fun createRandomData(): List<Pair<Float, Float>> {
        val data = mutableListOf<Pair<Float, Float>>()
        val random = java.util.Random()
        for (i in -40 until 1) {
            val kmAgo = i * .2f
            val last = data.lastOrNull()?.second ?: 0f
            val offset = when {
                last > 500f -> -300f
                last < 0f -> -100f
                else -> -200f
            }
            val efficiency = (last + random.nextFloat() * 400 + offset).coerceIn(-300f, 900f)
            data.add(Pair(kmAgo, efficiency))
        }
        return data.toList()
    }

    fun updateHistory(efficiencyHistory: List<Pair<Float, Float>>) {
        if (odoEfficiencyPairs == efficiencyHistory) return
        odoEfficiencyPairs = efficiencyHistory
        this.invalidate()
    }

    private fun kmAgoToX(kmAgo: Float): Float {
        return ((kmAgo - minKm) * width) / (maxKm - minKm)
    }

    private fun efficiencyToY(efficiency: Float): Float {
        return height - ((efficiency - minEfficiency) * height) / (maxEfficiency - minEfficiency)
    }
}
