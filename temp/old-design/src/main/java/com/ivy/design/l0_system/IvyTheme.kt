package com.ivy.design.l0_system

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
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
import androidx.compose.ui.text.TextStyle
import androidx.core.view.WindowCompat
import com.ivy.base.legacy.Theme
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
        // Type and shape tokens are derived from the M3 theme too, so legacy screens share one
        // type scale and one corner-radius scale with the Material 3 screens.
        val m3Typography = MaterialTheme.typography
        val m3Shapes = MaterialTheme.shapes
        val typography = remember(m3Typography) { m3Typography.toIvyTypography() }
        val shapes = remember(m3Shapes) { m3Shapes.toIvyShapes() }

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

/**
 * Maps the legacy ten-style type scale onto the Material 3 scale. The `n*` ("number") variants
 * get tabular figures so amounts stay column-aligned. Callers that pass `.style(...)` still
 * override weight and colour; size, line height and letter spacing now come from M3.
 */
private fun Typography.toIvyTypography(): IvyTypography = object : IvyTypography {
    override val h1 = displaySmall
    override val h2 = headlineLarge
    override val b1 = titleLarge
    override val b2 = bodyLarge
    override val c = labelMedium

    override val nH1 = displaySmall.tabular()
    override val nH2 = headlineLarge.tabular()
    override val nB1 = titleLarge.tabular()
    override val nB2 = bodyLarge.tabular()
    override val nC = labelMedium.tabular()
}

private fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = TabularFigures)

private const val TabularFigures = "tnum"

/**
 * Maps the legacy r1..r4 radii onto the Material 3 shape scale. r4 (the legacy card radius) maps
 * to `medium` rather than `small`, so legacy cards stay close to the M3 cards until they migrate.
 */
private fun Shapes.toIvyShapes(): IvyShapes = object : IvyShapes() {
    override val r1 = extraLarge
    override val r1Top = extraLarge.topOnly()
    override val r1Bot = extraLarge.bottomOnly()

    override val r2 = large
    override val r2Top = large.topOnly()
    override val r2Bot = large.bottomOnly()

    override val r3 = medium
    override val r3Top = medium.topOnly()
    override val r3Bot = medium.bottomOnly()

    override val r4 = medium
    override val r4Top = medium.topOnly()
    override val r4Bot = medium.bottomOnly()
}

private fun CornerBasedShape.topOnly(): CornerBasedShape =
    copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize)

private fun CornerBasedShape.bottomOnly(): CornerBasedShape =
    copy(topStart = ZeroCornerSize, topEnd = ZeroCornerSize)
