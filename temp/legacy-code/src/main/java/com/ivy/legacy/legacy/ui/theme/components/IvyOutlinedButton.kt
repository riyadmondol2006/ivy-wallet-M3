package com.ivy.wallet.ui.theme.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.Green

/** Native Material 3 [OutlinedButton] (pill). `solidBackground` fills with the M3 surface. */
@Composable
fun IvyOutlinedButton(
    modifier: Modifier = Modifier,
    text: String,
    @DrawableRes iconStart: Int?,
    solidBackground: Boolean = false,
    minWidth: Dp = Dp.Unspecified,
    minHeight: Dp = Dp.Unspecified,
    iconTint: Color = UI.colors.pureInverse,
    borderColor: Color = UI.colors.medium,
    textColor: Color = UI.colors.pureInverse,
    padding: Dp = 12.dp,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minWidth = minWidth, minHeight = minHeight),
        shape = RoundedCornerShape(percent = 50),
        border = BorderStroke(2.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (solidBackground) MaterialTheme.colorScheme.surface else Color.Transparent,
            contentColor = textColor,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = padding),
    ) {
        if (iconStart != null) {
            IvyIcon(icon = iconStart, tint = iconTint)
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun IvyOutlinedButtonFillMaxWidth(
    modifier: Modifier = Modifier,
    text: String,
    @DrawableRes iconStart: Int?,
    solidBackground: Boolean = false,
    iconTint: Color = UI.colors.pureInverse,
    borderColor: Color = UI.colors.medium,
    textColor: Color = UI.colors.pureInverse,
    padding: Dp = 16.dp,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(percent = 50),
        border = BorderStroke(2.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (solidBackground) MaterialTheme.colorScheme.surface else Color.Transparent,
            contentColor = textColor,
        ),
        contentPadding = PaddingValues(vertical = padding),
    ) {
        if (iconStart != null) {
            IvyIcon(icon = iconStart, tint = iconTint)
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, fontWeight = FontWeight.Bold)
    }
}

@Preview
@Composable
private fun Preview_FillMaxWidth() {
    IvyWalletComponentPreview {
        IvyOutlinedButtonFillMaxWidth(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = "Import backup file",
            iconStart = R.drawable.ic_export_csv,
            textColor = Green,
            iconTint = Green,
        ) {}
    }
}
