package com.ivy.design.l0_system

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.ivy.base.legacy.Theme
import com.ivy.design.api.IvyDesign
import com.ivy.design.system.IvyMaterial3Theme

@Deprecated("Old design system. Use `:ivy-design` and Material3")
val LocalIvyColors = compositionLocalOf<IvyColors> { error("No IvyColors") }

@Deprecated("Old design system. Use `:ivy-design` and Material3")
val LocalIvyTypography = compositionLocalOf<IvyTypography> { error("No IvyTypography") }

@Deprecated("Old design system. Use `:ivy-design` and Material3")
val LocalIvyShapes = compositionLocalOf<IvyShapes> { error("No IvyShapes") }

@Deprecated("Old design system. Use `:ivy-design` and Material3")
object UI {
    val colors: IvyColors
        @Composable
        @ReadOnlyComposable
        get() = LocalIvyColors.current

    val typo: IvyTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalIvyTypography.current

    val shapes: IvyShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalIvyShapes.current
}

/**
 * Legacy entry point, now **bridged onto Material 3 Expressive**.
 *
 * Instead of serving the old hardcoded palette, the deprecated `UI.colors` is now *derived from*
 * the active (and possibly dynamic / Material You) [MaterialTheme.colorScheme]. This means every
 * screen still written against the legacy `UI.*` design system automatically inherits the new
 * dynamic tonal theming with no per-file changes.
 */
@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun IvyTheme(
    theme: Theme,
    design: IvyDesign,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val dark = when (theme) {
        Theme.LIGHT -> false
        Theme.DARK, Theme.AMOLED_DARK -> true
        Theme.AUTO -> isDarkTheme
    }

    IvyMaterial3Theme(
        dark = dark,
        isTrueBlack = theme == Theme.AMOLED_DARK,
    ) {
        // MaterialTheme.colorScheme here is the dynamic / tonal Expressive scheme.
        val scheme = MaterialTheme.colorScheme
        val colors = remember(scheme) { scheme.toIvyColors() }
        val typography = design.typography()
        val shapes = design.shapes()

        CompositionLocalProvider(
            LocalIvyColors provides colors,
            LocalIvyTypography provides typography,
            LocalIvyShapes provides shapes
        ) {
            val view = LocalView.current
            if (!view.isInEditMode && view.context is Activity) {
                SideEffect {
                    val window = (view.context as Activity).window
                    window.statusBarColor = Color.Transparent.toArgb()
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                        colors.isLight
                }
            }

            content()
        }
    }
}

/**
 * Maps the Material 3 [ColorScheme] onto the legacy [IvyColors] surface so old screens render
 * with the dynamic tonal theme. Financial semantics follow the same mapping used app-wide:
 * income/green -> tertiary, expense/red -> error, primary -> primary.
 */
private fun ColorScheme.toIvyColors(): IvyColors = object : IvyColors {
    override val pure = surface
    override val pureInverse = onSurface

    override val gray = outline
    override val medium = surfaceVariant
    override val mediumInverse = inverseSurface

    override val primary = this@toIvyColors.primary
    override val primary1 = inversePrimary

    // Income tonal palette.
    override val green = tertiary
    override val green1 = tertiaryContainer

    override val orange = secondary
    override val orange1 = secondaryContainer

    // Expense tonal palette.
    override val red = error
    override val red1 = errorContainer
    override val red1Inverse = onErrorContainer

    override val isLight = surface.luminance() > 0.5f
}
