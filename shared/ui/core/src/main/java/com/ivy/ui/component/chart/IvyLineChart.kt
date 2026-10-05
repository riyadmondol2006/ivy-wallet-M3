package com.ivy.ui.component.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.design.system.IvyMotion
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.roundToInt

private val DefaultHeight = 200.dp
private val LineWidth = 2.5.dp
private val MarkerRadius = 5.dp
private val GridLabelGap = 6.dp
private val TooltipGap = 8.dp
private const val GridLines = 3
private const val FillAlphaTop = 0.35f
private const val PathTolerance = 0.001f

/**
 * Material 3 line chart: a primary-coloured line with a soft gradient fill, three horizontal
 * gridlines with value labels, a spring draw-in, and tap-to-inspect that reports the nearest
 * point through [onSelect]. Everything is themed from [MaterialTheme], nothing is hard-coded.
 */
@Composable
fun IvyLineChart(
    values: ImmutableList<Float>,
    modifier: Modifier = Modifier,
    height: Dp = DefaultHeight,
    selectedIndex: Int? = null,
    formatValue: (Float) -> String = { it.roundToInt().toString() },
    formatIndex: (Int) -> String = { "" },
    onSelect: (Int?) -> Unit = {},
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = MaterialTheme.colorScheme.primaryContainer
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tooltipColor = MaterialTheme.colorScheme.onSurface
    val labelStyle: TextStyle = MaterialTheme.typography.labelSmall
    val tooltipStyle: TextStyle = MaterialTheme.typography.labelMedium
    val textMeasurer = rememberTextMeasurer()

    val progress = remember { Animatable(0f) }
    LaunchedEffect(values) {
        progress.snapTo(0f)
        progress.animateTo(1f, IvyMotion.spatialSpring(PathTolerance))
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(values) {
                detectTapGestures { tap ->
                    if (values.size < 2) return@detectTapGestures
                    val stepX = size.width.toFloat() / (values.size - 1)
                    val index = (tap.x / stepX).roundToInt().coerceIn(0, values.lastIndex)
                    onSelect(if (index == selectedIndex) null else index)
                }
            },
    ) {
        if (values.isEmpty()) return@Canvas
        val min = values.min()
        val max = values.max()
        val span = (max - min).takeIf { it > 0f } ?: 1f
        val labelWidth = GridLines.let { lines ->
            (0 until lines).maxOf { i ->
                textMeasurer.measure(formatValue(min + span * i / (lines - 1)), labelStyle).size.width
            }
        } + GridLabelGap.toPx()
        val chartLeft = labelWidth
        val chartWidth = size.width - chartLeft
        val topPad = tooltipStyle.fontSize.toPx() * 2
        val chartHeight = size.height - topPad - labelStyle.fontSize.toPx()
        fun xAt(index: Int): Float {
            if (values.size == 1) return chartLeft + chartWidth / 2
            return chartLeft + chartWidth * index / (values.size - 1)
        }
        fun yAt(value: Float) = topPad + chartHeight * (1f - (value - min) / span)

        // Gridlines and value labels.
        for (i in 0 until GridLines) {
            val value = min + span * i / (GridLines - 1)
            val y = yAt(value)
            drawLine(gridColor, Offset(chartLeft, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            val label = textMeasurer.measure(formatValue(value), labelStyle)
            drawText(label, labelColor, Offset(0f, y - label.size.height / 2f))
        }

        // Line path, revealed along its length as the spring progresses.
        val fullPath = Path().apply {
            values.forEachIndexed { index, value ->
                if (index == 0) moveTo(xAt(0), yAt(value)) else lineTo(xAt(index), yAt(value))
            }
        }
        val measure = PathMeasure().apply { setPath(fullPath, false) }
        val shown = Path()
        measure.getSegment(0f, measure.length * progress.value, shown, true)

        val fill = Path().apply {
            addPath(shown)
            val endX = measure.getPosition(measure.length * progress.value).x
            lineTo(endX, topPad + chartHeight)
            lineTo(xAt(0), topPad + chartHeight)
            close()
        }
        drawPath(
            path = fill,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor.copy(alpha = FillAlphaTop), Color.Transparent),
                startY = topPad,
                endY = topPad + chartHeight,
            ),
        )
        drawPath(
            path = shown,
            color = lineColor,
            style = Stroke(width = LineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        // Selected point marker and tooltip.
        val index = selectedIndex?.takeIf { it in values.indices } ?: return@Canvas
        val point = Offset(xAt(index), yAt(values[index]))
        drawCircle(lineColor, radius = MarkerRadius.toPx(), center = point)
        drawCircle(fillColor, radius = MarkerRadius.toPx() / 2, center = point)
        val tooltip = textMeasurer.measure(
            text = listOf(formatIndex(index), formatValue(values[index])).filter { it.isNotEmpty() }
                .joinToString(separator = "  ·  "),
            style = tooltipStyle,
        )
        val tooltipX = (point.x - tooltip.size.width / 2f).coerceIn(chartLeft, size.width - tooltip.size.width)
        val tooltipY = (point.y - TooltipGap.toPx() - tooltip.size.height).coerceAtLeast(0f)
        drawText(tooltip, tooltipColor, Offset(tooltipX, tooltipY))
    }
}
