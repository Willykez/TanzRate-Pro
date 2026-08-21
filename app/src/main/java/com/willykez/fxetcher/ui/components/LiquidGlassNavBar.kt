package com.willykez.fxetcher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class GlassNavItem(
    val icon: ImageVector,
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

/**
 * A floating, translucent "glass" pill bottom bar with a sliding highlight
 * behind the selected item. True backdrop blur-through requires Android 13+
 * RenderEffect APIs that aren't reliably exposed in Compose yet, so the glass
 * look here is achieved with layered translucency, a soft top highlight, and
 * elevation — the same trick used by most frosted-glass UI before backdrop
 * blur became broadly available.
 */
@Composable
fun LiquidGlassNavBar(items: List<GlassNavItem>, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(32.dp)

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
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                )
            )
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(72.dp)) {
            val itemCount = items.size.coerceAtLeast(1)
            val slotWidth = maxWidth / itemCount
            val selectedIndex = items.indexOfFirst { it.selected }.coerceAtLeast(0)
            val indicatorOffset by animateDpAsState(
                targetValue = slotWidth * selectedIndex,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 380f),
                label = "glassIndicator"
            )

            androidx.compose.foundation.layout.Box(
                Modifier
                    .offset(x = indicatorOffset)
                    .padding(8.dp)
                    .width(slotWidth - 16.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            )

            Row(Modifier.fillMaxWidth().fillMaxHeight()) {
                items.forEach { item ->
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { item.onClick() },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = if (item.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AnimatedVisibility(visible = item.selected, enter = fadeIn(), exit = fadeOut()) {
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
