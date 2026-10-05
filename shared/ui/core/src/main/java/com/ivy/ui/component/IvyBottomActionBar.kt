package com.ivy.ui.component

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.design.system.IvySpacing
import com.ivy.ui.R

private val BarTonalElevation = 3.dp

/**
 * Bottom action bar anchored to the bottom of a screen: a tonal navigation (back/close) button on
 * the start and the screen's primary actions in [content]. Every list screen uses this so bottom
 * bars look and behave the same.
 */
@Composable
fun BoxScope.IvyBottomActionBar(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    navigationContentDescription: String = stringResource(R.string.back),
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = BarTonalElevation,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = IvySpacing.screenGutter, vertical = IvySpacing.itemGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = navigationIcon,
                    contentDescription = navigationContentDescription,
                )
            }

            Spacer(Modifier.width(IvySpacing.itemGap))

            content()
        }
    }
}
