package com.ivy.design.system

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Layout spacing tokens shared by every screen so gutters and gaps line up across the app.
 */
object IvySpacing {
    /** Horizontal inset between screen content and the window edge (M3 compact margin). */
    val screenGutter: Dp = 16.dp

    /** Horizontal inset inside bottom sheets and modals. */
    val sheetInset: Dp = 24.dp

    /** Vertical gap between list items and stacked cards. */
    val itemGap: Dp = 12.dp

    /** Gap between an icon and its label, or between tightly related elements. */
    val inlineGap: Dp = 8.dp
}
