package com.ivy.wallet.ui.theme.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.Gradient
import com.ivy.wallet.ui.theme.GradientRed
import com.ivy.wallet.ui.theme.White

/**
 * Native Material 3 circular icon button ([FilledIconButton]). With no [backgroundGradient] it
 * uses the theme's filled icon-button colours (primary); otherwise the gradient's start colour
 * becomes the solid container colour. [backgroundPadding], [horizontalGradient] and [hasShadow]
 * are kept for source compatibility.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun IvyCircleButton(
    modifier: Modifier = Modifier,
    backgroundPadding: Dp = 0.dp,
    backgroundGradient: Gradient? = null,
    horizontalGradient: Boolean = true,
    @DrawableRes icon: Int,
    tint: Color? = null,
    enabled: Boolean = true,
    hasShadow: Boolean = true,
    onClick: () -> Unit
) {
    val colors = if (backgroundGradient == null) {
        IconButtonDefaults.filledIconButtonColors()
    } else {
        IconButtonDefaults.filledIconButtonColors(
            containerColor = backgroundGradient.startColor,
            contentColor = tint ?: White,
        )
    }
    FilledIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = CircleShape,
        colors = colors,
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = "circle button",
            tint = tint ?: LocalContentColor.current,
        )
    }
}

@Preview
@Composable
private fun PreviewIvyCircleButton_Enabled() {
    IvyWalletComponentPreview {
        IvyCircleButton(
            icon = R.drawable.ic_delete,
            backgroundGradient = GradientRed,
            tint = White
        ) {
        }
    }
}

@Preview
@Composable
private fun PreviewIvyCircleButton_Disabled() {
    IvyWalletComponentPreview {
        IvyCircleButton(
            icon = R.drawable.ic_delete,
            backgroundGradient = GradientRed,
            enabled = false,
            tint = White
        ) {
        }
    }
}
