package com.willykez.fxetcher.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.text.DecimalFormat

/**
 * Displays a number that smoothly counts/rolls toward its target value whenever
 * it changes, instead of snapping — used for hero rate displays.
 */
@Composable
fun AnimatedRateText(
    value: Double?,
    suffix: String,
    style: TextStyle,
    color: Color,
    pattern: String = "#,##0.00",
    modifier: Modifier = Modifier
) {
    val fmt = remember(pattern) { DecimalFormat(pattern) }
    val animated by animateFloatAsState(
        targetValue = (value ?: 0.0).toFloat(),
        animationSpec = tween(durationMillis = 700),
        label = "animatedRate"
    )
    Text(
        text = if (value != null) "${fmt.format(animated)} $suffix" else "—",
        style = style,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

/** A shimmering placeholder bar shown in place of not-yet-loaded content. */
@Composable
fun ShimmerBar(
    modifier: Modifier = Modifier,
    barHeight: Dp = 16.dp,
    cornerRadius: Dp = 6.dp
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = -400f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(translate - 200f, 0f),
        end = Offset(translate + 200f, 0f)
    )
    Box(
        modifier
            .height(barHeight)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}
