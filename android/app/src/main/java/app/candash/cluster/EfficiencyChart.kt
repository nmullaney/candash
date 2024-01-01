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
    private var positiveColor: Int
    private var neutralColor: Int
    private val negativeColor = resources.getColor(R.color.telltale_green)

    init {
        val typedValue = TypedValue()

        context.theme.resolveAttribute(R.attr.colorSecondary, typedValue, true)
        positiveColor = typedValue.data

        context.theme.resolveAttribute(R.attr.colorGhost, typedValue, true)
        neutralColor = typedValue.data
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null) return

        this.alpha = 0.5f
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
        val fullPositiveColor = efficiencyToY(400f)

        val negativeShader =
            LinearGradient(
                0f,
                zeroY,
                0f,
                height.toFloat(),
                neutralColor,
                negativeColor,
                Shader.TileMode.CLAMP
            )
        val positiveShader =
            LinearGradient(
                0f,
                zeroY,
                0f,
                fullPositiveColor,
                neutralColor,
                positiveColor,
                Shader.TileMode.CLAMP
            )
        paint.style = Paint.Style.FILL

        // Set starting point of paths
        val negativePath = Path()
        negativePath.moveTo(firstX, zeroY)
        val positivePath = Path()
        positivePath.moveTo(firstX, zeroY)

        // Draw each path segment to the next point
        for (i in points.indices) {
            val point = points[i]
            // clamp last point.x to edge of screen
            val pointX = if (i == points.size - 1) width.toFloat() else point.x
            if (point.y >= zeroY) {
                negativePath.lineTo(pointX, point.y)
                positivePath.lineTo(pointX, zeroY)
            } else {
                negativePath.lineTo(pointX, zeroY)
                positivePath.lineTo(pointX, point.y)
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
