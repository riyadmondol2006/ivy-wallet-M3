package com.ivy.design.system

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
        content = content,
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
    primary = accent.primary,
    onPrimary = Color.White,
    primaryContainer = accent.extraLight,
    onPrimaryContainer = accent.extraDark,
    inversePrimary = accent.light,

    secondary = accent.kindaDark,
    onSecondary = Color.White,
    secondaryContainer = accent.light,
    onSecondaryContainer = accent.extraDark,

    // Income lives on tertiary.
    tertiary = IvyColors.Green.primary,
    onTertiary = Color.White,
    tertiaryContainer = IvyColors.Green.extraLight,
    onTertiaryContainer = IvyColors.Green.extraDark,

    // Expense lives on error.
    error = IvyColors.Red.primary,
    onError = Color.White,
    errorContainer = IvyColors.Red.extraLight,
    onErrorContainer = IvyColors.Red.extraDark,

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

    outline = IvyColors.Gray,
    outlineVariant = IvyColors.LightGray,
    scrim = Color.Black,
)

private fun ivyDarkColorScheme(accent: ColorShades, isTrueBlack: Boolean): ColorScheme = darkColorScheme(
    primary = accent.light,
    onPrimary = accent.extraDark,
    primaryContainer = accent.dark,
    onPrimaryContainer = accent.extraLight,
    inversePrimary = accent.primary,

    secondary = accent.kindaLight,
    onSecondary = accent.extraDark,
    secondaryContainer = accent.kindaDark,
    onSecondaryContainer = accent.extraLight,

    // Income lives on tertiary.
    tertiary = IvyColors.Green.kindaLight,
    onTertiary = IvyColors.Green.extraDark,
    tertiaryContainer = IvyColors.Green.dark,
    onTertiaryContainer = IvyColors.Green.extraLight,

    // Expense lives on error.
    error = IvyColors.Red.kindaLight,
    onError = IvyColors.Red.extraDark,
    errorContainer = IvyColors.Red.dark,
    onErrorContainer = IvyColors.Red.extraLight,

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

    outline = IvyColors.Gray,
    outlineVariant = IvyColors.DarkGray,
    scrim = Color.Black,
)

/** Deep charcoal with a faint plum tint — the default Expressive dark background. */
private val MidnightPlum = Color(0xFF14121A)
// endregion
