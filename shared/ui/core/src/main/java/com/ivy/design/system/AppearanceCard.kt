package com.ivy.design.system

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Self-contained Material 3 "Appearance" control: a system-color (Material You) switch plus an
 * accent-color picker. Reads/writes [IvyThemeController], so changes re-theme the whole app live.
 * Drop it into any settings list as a single item.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceCard(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val useDynamic = IvyThemeController.useDynamicColor && dynamicSupported
    val accent = IvyThemeController.accent

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(20.dp),
    ) {
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (dynamicSupported) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Use system color",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Material You from your wallpaper",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = IvyThemeController.useDynamicColor,
                    onCheckedChange = { IvyThemeController.setUseDynamicColor(context, it) },
                )
            }
        }

        AnimatedVisibility(visible = !useDynamic) {
            Column {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Accent color",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IvyAccent.entries.forEach { option ->
                        AccentSwatch(
                            color = option.shades.primary,
                            selected = option == accent,
                            onClick = {
                                // Choosing an accent implies turning off dynamic color.
                                IvyThemeController.setUseDynamicColor(context, false)
                                IvyThemeController.setAccent(context, option)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccentSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val ring = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected) {
                    Modifier.border(3.dp, ring, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "selected",
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
            )
        }
    }
}
