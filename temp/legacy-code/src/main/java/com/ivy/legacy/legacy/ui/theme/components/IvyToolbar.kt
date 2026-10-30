package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Deprecated("Old design system. Use `:ivy-design` and Material3")
enum class BackButtonType {
    BACK, CLOSE
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun IvyToolbar(
    onBack: () -> Unit,
    backButtonType: BackButtonType = BackButtonType.BACK,
    paddingTop: Dp = 16.dp,
    paddingBottom: Dp = 16.dp,
    Content: @Composable RowScope.() -> Unit = { }
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Opaque M3 surface so sticky toolbars don't show scrolled content through them.
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = paddingTop, bottom = paddingBottom),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(20.dp))

        when (backButtonType) {
            BackButtonType.BACK -> {
                BackButton(
                    modifier = Modifier.testTag("toolbar_back")
                ) {
                    onBack()
                }
            }

            BackButtonType.CLOSE -> {
                CloseButton(
                    modifier = Modifier.testTag("toolbar_close")
                ) {
                    onBack()
                }
            }
        }

        Content()
    }
}
