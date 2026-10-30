package com.ivy.design.system

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import com.ivy.design.system.colors.ColorShades
import com.ivy.design.system.colors.IvyColors

/**
 * Material 3 Expressive theme for Ivy Wallet.
 *
 * Color strategy:
 * - On Android 12+ (API 31) the scheme is generated from the user's wallpaper via
 *   [dynamicLightColorScheme] / [dynamicDarkColorScheme] (Material You).
 * - Below API 31 we fall back to a hand-tuned Expressive scheme seeded from [IvyColors].
 *
 * Dark mode uses deeply tinted **tonal** surfaces (deep charcoal / midnight-plum) instead of
 * the legacy true-black. True black (#000000) is now an explicit AMOLED opt-in
 * ([isTrueBlack]) rather than the default.
 *
 * Financial semantics are mapped onto the tonal palette so they harmonize with the user's
 * generated theme: income -> [ColorScheme.income] (tertiary), expense -> [ColorScheme.expense]
 * (error), transfers/neutral -> [ColorScheme.transfer] (primary). See the extensions below.
 *
 * Note: Material3 1.4.0 still keeps `MaterialExpressiveTheme` / `MaterialShapes` `internal`, so
 * we wrap the standard [MaterialTheme] with the Expressive [IvyExpressiveTypography] /
 * [IvyExpressiveShapes] and apply Expressive *motion* (springs) at the component level.
 */
@Composable
fun IvyMaterial3Theme(
    isTrueBlack: Boolean,
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    // Appearance preference: use Material You (wallpaper) color, or a hand-picked accent.
    val useDynamic = IvyThemeController.useDynamicColor &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val accent = IvyThemeController.accent.shades

    val colorScheme = when {
        useDynamic && dark -> dynamicDarkColorScheme(context).withTonalDark(isTrueBlack)
        useDynamic && !dark -> dynamicLightColorScheme(context)
        dark -> ivyDarkColorScheme(accent, isTrueBlack)
        else -> ivyLightColorScheme(accent)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = IvyExpressiveTypography,
        shapes = IvyExpressiveShapes,
    ) {
        CompactDensity(enabled = IvyThemeController.compactUi, content = content)
    }
}

/** How much smaller everything is drawn in compact mode (dp and sp alike). */
private const val CompactScale = 0.88f

/** True once an outer theme has already scaled the density, so nested themes don't shrink it again. */
@Suppress("CompositionLocalAllowlist") // private guard; nothing outside this file reads it
private val LocalCompactApplied = staticCompositionLocalOf { false }

/**
 * Compact mode scales the density for everything below, so every screen (including legacy ones
 * with fixed sizes) gets tighter spacing, smaller components and smaller text in proportion.
 * Layouts measure in this density too, so nothing overlaps or clips.
 */
@Composable
private fun CompactDensity(enabled: Boolean, content: @Composable () -> Unit) {
    val alreadyApplied = LocalCompactApplied.current
    val scale = enabled && !alreadyApplied
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    val minInteractive = LocalMinimumInteractiveComponentSize.current
    // Same composition structure whether compact or not, so toggling it keeps screen state
    // (scroll positions, open screens) instead of rebuilding everything below.
    CompositionLocalProvider(
        LocalDensity provides if (scale) Density(density.density * CompactScale, density.fontScale) else density,
        // Touch targets must stay 48dp on the physical screen: in the scaled density that's
        // 48 / CompactScale "dp". Both the M3 layout minimum and the hit-test minimum are enlarged.
        LocalMinimumInteractiveComponentSize provides if (scale) minInteractive / CompactScale else minInteractive,
        LocalViewConfiguration provides if (scale) {
            CompactViewConfiguration(viewConfiguration)
        } else {
            viewConfiguration
        },
        LocalCompactApplied provides (alreadyApplied || scale),
        content = content,
    )
}

/** Same as [base], but its minimum touch target keeps its physical size under compact density. */
private class CompactViewConfiguration(base: ViewConfiguration) : ViewConfiguration by base {
    override val minimumTouchTargetSize: DpSize = DpSize(
        width = base.minimumTouchTargetSize.width / CompactScale,
        height = base.minimumTouchTargetSize.height / CompactScale,
    )
}

// region Semantic financial accessors ------------------------------------------------------------
// One source of truth shared by features and the legacy `UI.colors` bridge. Reading from the
// active (possibly dynamic) scheme keeps financial colors in harmony with the device theme.

/** Money coming in. Mapped to the tertiary tonal palette. */
val ColorScheme.income: Color get() = tertiary
val ColorScheme.onIncome: Color get() = onTertiary
val ColorScheme.incomeContainer: Color get() = tertiaryContainer
val ColorScheme.onIncomeContainer: Color get() = onTertiaryContainer

/** Money going out. Mapped to the error tonal palette. */
val ColorScheme.expense: Color get() = error
val ColorScheme.onExpense: Color get() = onError
val ColorScheme.expenseContainer: Color get() = errorContainer
val ColorScheme.onExpenseContainer: Color get() = onErrorContainer

/** Transfers / neutral money movement. Mapped to the primary tonal palette. */
val ColorScheme.transfer: Color get() = primary
val ColorScheme.onTransfer: Color get() = onPrimary
// endregion

/**
 * Re-tints a dynamic dark scheme so backgrounds use deep tonal surfaces. When [isTrueBlack] is
 * set (AMOLED), only the base background/surface go pure black while elevated containers keep
 * their tonal tint for legibility.
 */
private fun ColorScheme.withTonalDark(isTrueBlack: Boolean): ColorScheme {
    if (!isTrueBlack) return this
    return copy(
        background = IvyColors.TrueBlack,
        surface = IvyColors.TrueBlack,
        surfaceContainerLowest = IvyColors.TrueBlack,
    )
}

// region Contrast helpers --------------------------------------------------------------------------
// The accent swatches are picked for looks, not legibility (bright yellow, green, blue, red). The
// static schemes pass them through these so filled-button labels and accent-coloured text both
// reach WCAG 4.5:1.

private val ReadableDark = Color(0xFF1B1B1F)
private const val MinTextContrast = 4.5f

/** WCAG 2.x contrast-ratio offset (accounts for viewing flare). */
private const val LuminanceFlare = 0.05f

private fun contrast(a: Color, b: Color): Float {
    val la = a.luminance() + LuminanceFlare
    val lb = b.luminance() + LuminanceFlare
    return maxOf(la, lb) / minOf(la, lb)
}

/**
 * Content colour for text/icons on [background]: white or a soft near-black when they reach
 * WCAG 4.5:1, pure black as the last resort, else whichever contrasts most.
 */
fun readableContentColor(background: Color): Color {
    val candidates = listOf(Color.White, ReadableDark, Color.Black)
    return candidates.firstOrNull { contrast(it, background) >= MinTextContrast }
        ?: candidates.maxBy { contrast(it, background) }
}

private fun readableOn(background: Color): Color = readableContentColor(background)

/** [tint] when it is legible on [background], otherwise plain white/near-black. */
private fun readableTint(tint: Color, background: Color): Color =
    if (contrast(tint, background) >= MinTextContrast) tint else readableOn(background)

/**
 * [color] itself when it reads as text on [background] (4.5:1), otherwise the closest version of
 * it nudged towards white (dark backgrounds) or black (light ones) that does.
 */
private fun readableShade(color: Color, background: Color): Color {
    if (contrast(color, background) >= MinTextContrast) return color
    val target = if (background.luminance() < HalfLuminance) Color.White else Color.Black
    return (1..ShadeSteps).asSequence()
        .map { step -> lerp(color, target, step.toFloat() / ShadeSteps) }
        .firstOrNull { contrast(it, background) >= MinTextContrast }
        ?: target
}

/**
 * The lightest shade from [from] downwards that is readable as text (4.5:1, like M3's tone-40
 * primary) on the darkest static light surface; primary is used for text and icons as well as
 * fills. Falls back to a darkened extraDark when even that isn't enough (yellow).
 */
private fun ColorShades.onLightSurface(from: Color = primary): Color =
    listOf(primary, kindaDark, dark, extraDark)
        .dropWhile { it != from }
        .ifEmpty { listOf(from, dark, extraDark) }
        .firstOrNull { contrast(it, LightSurfaceContainerHighest) >= MinTextContrast }
        ?: readableShade(extraDark, LightSurfaceContainerHighest)

/** [color], lightened if needed, so it reads as text on the brightest static dark surface. */
private fun onDarkSurface(color: Color): Color = readableShade(color, DarkSurfaceContainerHighest)

private const val HalfLuminance = 0.5f
private const val ShadeSteps = 20
// endregion

// region Static fallback schemes (API < 31) ------------------------------------------------------
// Seeded from IvyColors but expressed as full M3 tonal schemes. No pure-black backgrounds unless
// the AMOLED opt-in is active.

private val LightSurfaceContainerLow = Color(0xFFF6F4FB)
private val LightSurfaceContainer = Color(0xFFF1EEF8)
private val LightSurfaceContainerHigh = Color(0xFFEBE7F3)
private val LightSurfaceContainerHighest = Color(0xFFE5E0EF)
private val DarkSurfaceVariant = Color(0xFF2A2630)
private val DarkSurfaceContainerLowest = Color(0xFF0E0C13)
private val DarkSurfaceContainerLow = Color(0xFF1A1722)
private val DarkSurfaceContainer = Color(0xFF1E1B27)
private val DarkSurfaceContainerHigh = Color(0xFF292532)
private val DarkSurfaceContainerHighest = Color(0xFF34303D)

private fun ivyLightColorScheme(accent: ColorShades): ColorScheme = lightColorScheme(
    primary = accent.onLightSurface(),
    onPrimary = readableOn(accent.onLightSurface()),
    primaryContainer = accent.extraLight,
    onPrimaryContainer = readableTint(accent.extraDark, accent.extraLight),
    inversePrimary = readableShade(accent.light, IvyColors.DarkGray),

    secondary = accent.onLightSurface(from = accent.kindaDark),
    onSecondary = readableOn(accent.onLightSurface(from = accent.kindaDark)),
    secondaryContainer = accent.light,
    onSecondaryContainer = readableTint(accent.extraDark, accent.light),

    // Income lives on tertiary.
    tertiary = IvyColors.Green.onLightSurface(),
    onTertiary = readableOn(IvyColors.Green.onLightSurface()),
    tertiaryContainer = IvyColors.Green.extraLight,
    onTertiaryContainer = readableTint(IvyColors.Green.extraDark, IvyColors.Green.extraLight),

    // Expense lives on error.
    error = IvyColors.Red.onLightSurface(),
    onError = readableOn(IvyColors.Red.onLightSurface()),
    errorContainer = IvyColors.Red.extraLight,
    onErrorContainer = readableTint(IvyColors.Red.extraDark, IvyColors.Red.extraLight),

    background = IvyColors.White,
    onBackground = IvyColors.Black,
    surface = IvyColors.White,
    onSurface = IvyColors.Black,
    surfaceVariant = IvyColors.ExtraLightGray,
    onSurfaceVariant = IvyColors.DarkGray,
    surfaceTint = accent.primary,
    inverseSurface = IvyColors.DarkGray,
    inverseOnSurface = IvyColors.White,

    surfaceContainerLowest = Color.White,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    // Pin the darkest light surface to the one accent text is contrast-checked against.
    surfaceDim = LightSurfaceContainerHighest,

    outline = IvyColors.Gray,
    outlineVariant = IvyColors.LightGray,
    scrim = Color.Black,
)

private fun ivyDarkColorScheme(accent: ColorShades, isTrueBlack: Boolean): ColorScheme = darkColorScheme(
    primary = onDarkSurface(accent.light),
    onPrimary = readableTint(accent.extraDark, onDarkSurface(accent.light)),
    primaryContainer = accent.dark,
    onPrimaryContainer = readableTint(accent.extraLight, accent.dark),
    inversePrimary = readableShade(accent.primary, IvyColors.ExtraLightGray),

    secondary = onDarkSurface(accent.kindaLight),
    onSecondary = readableTint(accent.extraDark, onDarkSurface(accent.kindaLight)),
    secondaryContainer = accent.kindaDark,
    onSecondaryContainer = readableTint(accent.extraLight, accent.kindaDark),

    // Income lives on tertiary.
    tertiary = onDarkSurface(IvyColors.Green.kindaLight),
    onTertiary = readableTint(IvyColors.Green.extraDark, onDarkSurface(IvyColors.Green.kindaLight)),
    tertiaryContainer = IvyColors.Green.dark,
    onTertiaryContainer = readableTint(IvyColors.Green.extraLight, IvyColors.Green.dark),

    // Expense lives on error.
    error = onDarkSurface(IvyColors.Red.kindaLight),
    onError = readableTint(IvyColors.Red.extraDark, onDarkSurface(IvyColors.Red.kindaLight)),
    errorContainer = IvyColors.Red.dark,
    onErrorContainer = readableTint(IvyColors.Red.extraLight, IvyColors.Red.dark),

    // Deep tonal "midnight-plum" surfaces instead of true black.
    background = if (isTrueBlack) IvyColors.TrueBlack else MidnightPlum,
    onBackground = IvyColors.White,
    surface = if (isTrueBlack) IvyColors.TrueBlack else MidnightPlum,
    onSurface = IvyColors.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = IvyColors.LightGray,
    surfaceTint = accent.light,
    inverseSurface = IvyColors.ExtraLightGray,
    inverseOnSurface = MidnightPlum,

    surfaceContainerLowest = if (isTrueBlack) IvyColors.TrueBlack else DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    // Pin the brightest dark surface to the one accent text is contrast-checked against.
    surfaceBright = DarkSurfaceContainerHighest,

    outline = IvyColors.Gray,
    outlineVariant = IvyColors.DarkGray,
    scrim = Color.Black,
)

/** Deep charcoal with a faint plum tint — the default Expressive dark background. */
private val MidnightPlum = Color(0xFF14121A)
// endregion
