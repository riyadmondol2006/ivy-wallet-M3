package com.ivy.wallet.ui.theme.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.Gradient

/** Native Material 3 outlined circular icon button. */
@Composable
fun CloseButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    CircleButton(
        modifier = modifier,
        icon = R.drawable.ic_dismiss,
        contentDescription = "close",
        onClick = onClick,
    )
}

@Composable
fun CircleButton(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    contentDescription: String = "icon",
    backgroundColor: Color = UI.colors.pure,
    borderColor: Color = UI.colors.medium,
    tint: Color? = UI.colors.pureInverse,
    onClick: () -> Unit,
) {
    OutlinedIconButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        border = BorderStroke(2.dp, borderColor),
        colors = IconButtonDefaults.outlinedIconButtonColors(containerColor = backgroundColor),
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = contentDescription,
            tint = tint ?: Color.Unspecified,
        )
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun CircleButtonFilled(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    contentDescription: String = "icon",
    backgroundColor: Color = UI.colors.medium,
    tint: Color? = UI.colors.pureInverse,
    clickAreaPadding: Dp = 8.dp,
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = backgroundColor),
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = contentDescription,
            tint = tint ?: Color.Unspecified,
        )
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun CircleButtonFilledGradient(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    contentDescription: String = "icon",
    iconPadding: Dp = 8.dp,
    backgroundGradient: Gradient = Gradient.solid(UI.colors.medium),
    tint: Color? = UI.colors.pureInverse,
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = backgroundGradient.startColor,
        ),
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = contentDescription,
            tint = tint ?: Color.Unspecified,
        )
    }
}

@Composable
fun BackButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    CircleButton(
        modifier = modifier,
        icon = R.drawable.ic_back,
        contentDescription = "back",
        onClick = onClick,
    )
}

@Preview
@Composable
private fun PreviewCloseButton() {
    IvyWalletComponentPreview {
        CloseButton {
        }
    }
}

@Preview
@Composable
private fun PreviewCircleButtonFilled() {
    IvyWalletComponentPreview {
        CircleButtonFilled(
            icon = R.drawable.ic_sort_by_alpha_24,
            onClick = {},
            clickAreaPadding = 12.dp,
        )
    }
}
