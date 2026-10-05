package com.ivy.wallet.ui.theme.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.Gradient
import com.ivy.wallet.ui.theme.Ivy
import com.ivy.wallet.ui.theme.White

/**
 * Native Material 3 filled button kept for the legacy call sites.
 *
 * With no [backgroundGradient] it is a standard M3 filled button in the theme's primary colour
 * (so it follows dynamic colour and the accent setting). When a gradient is passed, its start
 * colour becomes the solid container colour, as before. Likewise [textStyle] and [iconTint]
 * default to the M3 button content style and colour unless the caller overrides them.
 *
 * Several legacy-only knobs ([shadowAlpha], [wrapContentMode], [hasGlow], [iconEdgePadding]) are
 * retained for source compatibility but are no-ops under the M3 [Button].
 */
@Suppress("UNUSED_PARAMETER", "LongParameterList")
@Composable
fun IvyButton(
    modifier: Modifier = Modifier,
    text: String,
    backgroundGradient: Gradient? = null,
    textStyle: TextStyle? = null,
    @DrawableRes iconStart: Int? = null,
    @DrawableRes iconEnd: Int? = null,
    iconTint: Color? = null,
    enabled: Boolean = true,
    shadowAlpha: Float = 0.15f,
    wrapContentMode: Boolean = true,
    hasGlow: Boolean = true,
    padding: Dp = 12.dp,
    iconEdgePadding: Dp = 12.dp,
    iconTextPadding: Dp = 4.dp,
    onClick: () -> Unit
) {
    val colors = if (backgroundGradient == null) {
        ButtonDefaults.buttonColors()
    } else {
        ButtonDefaults.buttonColors(
            containerColor = backgroundGradient.startColor,
            contentColor = iconTint ?: White,
        )
    }
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = padding),
    ) {
        val contentColor = iconTint ?: LocalContentColor.current
        if (iconStart != null) {
            Icon(
                painter = painterResource(id = iconStart),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
                tint = contentColor,
            )
            Spacer(Modifier.width(iconTextPadding))
        }

        Text(text = text, style = textStyle ?: LocalTextStyle.current)

        if (iconEnd != null) {
            Spacer(Modifier.width(iconTextPadding))
            Icon(
                painter = painterResource(id = iconEnd),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
                tint = contentColor,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewIvyButtonWrapContentWithIconStart() {
    IvyWalletComponentPreview {
        IvyButton(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .wrapContentSize(),
            iconStart = R.drawable.ic_plus,
            text = "Add new",
            wrapContentMode = true
        ) {
        }
    }
}

@Preview
@Composable
private fun PreviewIvyButtonFillMaxWidth() {
    IvyWalletComponentPreview {
        IvyButton(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            backgroundGradient = Gradient(Ivy, Ivy),
            iconEnd = R.drawable.ic_onboarding_next_arrow,
            text = "Category 1",
            wrapContentMode = false
        ) {
        }
    }
}
