package com.ivy.legacy.legacy.ui.theme.components

import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.design.system.readableContentColor

/**
 * The item's own colour as a circular avatar with its icon on top (as on the Accounts tab), so
 * screens keep the account/category/loan identity without painting whole areas in that colour.
 */
@Composable
fun ItemAvatar(
    iconName: String?,
    @DrawableRes defaultIcon: Int,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        ItemIconSDefaultIcon(
            iconName = iconName,
            defaultIcon = defaultIcon,
            tint = readableContentColor(color),
        )
    }
}
