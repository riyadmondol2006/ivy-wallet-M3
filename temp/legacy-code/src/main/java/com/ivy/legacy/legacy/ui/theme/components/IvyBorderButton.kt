package com.ivy.wallet.ui.theme.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.Gradient

/**
 * Native Material 3 [OutlinedButton]. The legacy gradient border is dropped for a clean M3 pill
 * with the standard 1dp outline. [backgroundGradient]/[wrapContentMode] are retained for source
 * compatibility but are no-ops under the M3 button.
 */
@Suppress("UNUSED_PARAMETER", "LongParameterList")
@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun IvyBorderButton(
    modifier: Modifier = Modifier,
    text: String,
    textStyle: TextStyle = UI.typo.b2.style(
        color = UI.colors.pureInverse,
        fontWeight = FontWeight.Bold
    ),
    backgroundGradient: Gradient = Gradient.solid(UI.colors.mediumInverse),
    @DrawableRes iconStart: Int? = null,
    @DrawableRes iconEnd: Int? = null,
    iconTint: Color = UI.colors.pureInverse,
    enabled: Boolean = true,
    wrapContentMode: Boolean = true,

    padding: Dp = 12.dp,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(percent = 50),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = iconTint),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = padding),
    ) {
        if (iconStart != null) {
            Icon(
                painter = painterResource(id = iconStart),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
                tint = iconTint,
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        }

        Text(text = text, style = textStyle)

        if (iconEnd != null) {
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Icon(
                painter = painterResource(id = iconEnd),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
                tint = iconTint,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewIvyBorderButton() {
    IvyWalletComponentPreview {
        IvyBorderButton(
            text = "New label",
            iconStart = R.drawable.ic_label_hashtag
        ) {
        }
    }
}
