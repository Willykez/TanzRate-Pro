package com.willykez.fxetcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/** Minimal line-chart used for the small trend indicator on rate rows. */
@Composable
fun Sparkline(points: List<Double>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val min = points.min()
        val max = points.max()
        val range = (max - min).takeIf { it > 0 } ?: 1.0
        val stepX = size.width / (points.size - 1)

        val path = androidx.compose.ui.graphics.Path()
        points.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - ((v - min) / range * size.height).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = Stroke(width = 3f, cap = StrokeCap.Round))

        // Endpoint dot
        val lastX = (points.size - 1) * stepX
        val lastY = size.height - ((points.last() - min) / range * size.height).toFloat()
        drawCircle(color, radius = 4f, center = Offset(lastX, lastY))
    }
}

/** A larger sparkline used inside detail sheets, with a filled gradient under the line. */
@Composable
fun AreaSparkline(points: List<Double>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val min = points.min()
        val max = points.max()
        val range = (max - min).takeIf { it > 0 } ?: 1.0
        val stepX = size.width / (points.size - 1)

        val linePath = androidx.compose.ui.graphics.Path()
        val fillPath = androidx.compose.ui.graphics.Path()
        points.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - ((v - min) / range * size.height).toFloat()
            if (i == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(size.width, size.height)
        fillPath.close()

        drawPath(fillPath, color = color.copy(alpha = 0.15f))
        drawPath(linePath, color = color, style = Stroke(width = 5f, cap = StrokeCap.Round))
    }
}

/**
 * Overlays two currencies' trends on one chart. Since raw TZS values live on
 * wildly different scales (e.g. USD vs KES), each series is rebased to its
 * own percent-change-from-first-point before plotting, so the two lines are
 * directly comparable as relative performance rather than absolute value.
 */
@Composable
fun DualTrendChart(
    pointsA: List<Double>,
    pointsB: List<Double>,
    colorA: Color,
    colorB: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (pointsA.size < 2 || pointsB.size < 2) return@Canvas

        fun rebase(points: List<Double>): List<Double> {
            val first = points.first()
            return if (first == 0.0) points.map { 0.0 } else points.map { (it - first) / first * 100.0 }
        }

        val seriesA = rebase(pointsA)
        val seriesB = rebase(pointsB)
        val allValues = seriesA + seriesB
        val min = allValues.min()
        val max = allValues.max()
        val range = (max - min).takeIf { it > 0 } ?: 1.0

        fun drawSeries(series: List<Double>, color: Color) {
            val stepX = size.width / (series.size - 1)
            val path = androidx.compose.ui.graphics.Path()
            series.forEachIndexed { i, v ->
                val x = i * stepX
                val y = size.height - ((v - min) / range * size.height).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = color, style = Stroke(width = 4.5f, cap = StrokeCap.Round))
            val lastX = (series.size - 1) * stepX
            val lastY = size.height - ((series.last() - min) / range * size.height).toFloat()
            drawCircle(color, radius = 5f, center = Offset(lastX, lastY))
        }

        // A zero baseline helps read which currency is up/down vs its own starting point.
        val zeroY = size.height - ((0.0 - min) / range * size.height).toFloat()
        drawLine(
            color = Color.Gray.copy(alpha = 0.3f),
            start = Offset(0f, zeroY),
            end = Offset(size.width, zeroY),
            strokeWidth = 1.5f
        )

        drawSeries(seriesA, colorA)
        drawSeries(seriesB, colorB)
    }
}
