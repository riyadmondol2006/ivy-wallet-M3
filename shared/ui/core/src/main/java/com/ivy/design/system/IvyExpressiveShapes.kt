package com.ivy.design.system

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive shape scale for Ivy Wallet.
 *
 * Expressive favors generous, organic corner rounding over the legacy small, rigid radii.
 * These tokens feed [androidx.compose.material3.MaterialTheme.shapes] and (via the legacy
 * bridge) the deprecated `UI.shapes`, so the whole app rounds consistently.
 */
val IvyExpressiveShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)
