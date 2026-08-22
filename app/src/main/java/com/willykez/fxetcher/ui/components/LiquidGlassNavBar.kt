package com.willykez.fxetcher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

data class GlassNavItem(
    val icon: ImageVector,
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

/**
 * A floating, translucent "glass" pill bottom bar in the spirit of iOS's
 * Liquid Glass tab bar: a bouncy spring-driven indicator that you can either
 * tap to jump to directly, or drag your finger along the bar to slide it
 * between tabs, snapping to the nearest one with a satisfying overshoot on
 * release.
 *
 * True backdrop blur-through (seeing blurred content behind the bar) needs
 * Android 13+ RenderEffect APIs not yet reliably exposed in Compose, so the
 * glass look here is layered translucency + a soft highlight + elevation —
 * the same technique frosted-glass UIs used before backdrop blur existed.
 */
@Composable
fun LiquidGlassNavBar(items: List<GlassNavItem>, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(32.dp)
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val itemCount = items.size.coerceAtLeast(1)
    val selectedIndex = items.indexOfFirst { it.selected }.coerceAtLeast(0)

    val bouncySpring = spring<Float>(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)

    Column(
        modifier
            .shadow(elevation = 18.dp, shape = shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.25f))
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.80f)
                    )
                )
            )
            .background(
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent))
            )
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(72.dp)) {
            val slotWidthDp = maxWidth / itemCount
            val slotWidthPx = with(density) { slotWidthDp.toPx() }

            var isDragging by remember { mutableStateOf(false) }
            var dragOffsetPx by remember { mutableFloatStateOf(0f) }

            fun nearestIndex(offsetPx: Float): Int =
                (offsetPx / slotWidthPx).roundToInt().coerceIn(0, itemCount - 1)

            // A single touch surface spanning the whole bar: a quick tap jumps
            // straight to that tab, a horizontal drag slides the live highlight
            // and snaps to the nearest tab on release.
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .pointerInput(itemCount) {
                        detectTapGestures { offset ->
                            val idx = nearestIndex(offset.x)
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            items.getOrNull(idx)?.onClick?.invoke()
                        }
                    }
                    .pointerInput(itemCount) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                isDragging = true
                                dragOffsetPx = slotWidthPx * selectedIndex
                            },
                            onDragEnd = {
                                isDragging = false
                                val idx = nearestIndex(dragOffsetPx)
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                items.getOrNull(idx)?.onClick?.invoke()
                            },
                            onDragCancel = { isDragging = false },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(0f, slotWidthPx * (itemCount - 1))
                            }
                        )
                    }
            ) {
                Row(Modifier.fillMaxWidth().fillMaxHeight()) {
                    items.forEachIndexed { index, item ->
                        val isLive = if (isDragging) nearestIndex(dragOffsetPx) == index else item.selected
                        val iconScale by animateFloatAsState(
                            targetValue = if (isLive) 1.15f else 1f,
                            animationSpec = bouncySpring,
                            label = "iconBounce$index"
                        )
                        Column(
                            Modifier.weight(1f).fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                item.icon,
                                contentDescription = item.label,
                                tint = if (isLive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.graphicsLayer { scaleX = iconScale; scaleY = iconScale }
                            )
                            AnimatedVisibility(visible = isLive, enter = fadeIn(), exit = fadeOut()) {
                                Text(
                                    item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
