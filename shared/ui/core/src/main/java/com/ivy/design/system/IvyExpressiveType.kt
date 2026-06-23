package com.ivy.design.system

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Material 3 typography for the redesign — authentic "Google" look on **Roboto**.
 *
 * Both faces resolve to [FontFamily.Default], which on Android is the system **Roboto** family.
 * Roboto ships every weight from Thin (100) to Black (900) as a system resource, so the
 * [financialNumberStyle] helper can still step the big balance number across the full weight
 * range without bundling a binary asset.
 *
 * If a true variable font is desired later (smooth `wght` morphing of the balance number), drop
 * `roboto_flex.ttf` into `res/font/` and switch [DisplayFamily] to
 * `variableFontFamily(R.font.roboto_flex)` — the helper already drives a continuous
 * [FontVariation.weight] axis. The bundled Raleway/Open Sans weights remain in `res/font/` for
 * any legacy preview that references them directly, but the live type scale no longer uses them.
 */

/** Display/headline face — system Roboto. */
val DisplayFamily: FontFamily = FontFamily.Default

/** Body/label face — system Roboto. */
val BodyFamily: FontFamily = FontFamily.Default

private const val MinFontWeight = 1
private const val MaxFontWeight = 1000
private val fontWeightSteps = listOf(100, 200, 300, 400, 500, 600, 700, 800, 900, MaxFontWeight)

/**
 * Builds a true variable-font family from a single ttf that exposes a `wght` axis. Each entry
 * pins the axis so Compose can interpolate; use with a font that actually carries the axis.
 */
@Suppress("unused")
@OptIn(ExperimentalTextApi::class)
fun variableFontFamily(resId: Int): FontFamily = FontFamily(
    fontWeightSteps.map { w ->
        Font(
            resId = resId,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w)),
        )
    }
)

/**
 * Style for the big, expressive financial numbers (e.g. the Home total balance).
 *
 * @param weight continuous 1..1000 weight. With a variable font this morphs smoothly via the
 * `wght` axis; with the static fallback it snaps to the nearest available cut.
 */
@Composable
fun financialNumberStyle(
    fontSize: TextUnit,
    weight: Int = MaxFontWeight,
): TextStyle = remember(fontSize, weight) {
    TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight(weight.coerceIn(MinFontWeight, MaxFontWeight)),
        fontSize = fontSize,
        letterSpacing = (-0.02).em,
        lineHeight = fontSize * 1.05f,
    )
}

private val tightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

/**
 * Material 3 Expressive type scale. Display/headline use the heavy expressive [DisplayFamily];
 * body/label use [BodyFamily]. Fed to `MaterialExpressiveTheme` and mirrored into the legacy
 * `UI.typo` bridge.
 */
val IvyExpressiveTypography: Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
        lineHeightStyle = tightLineHeight,
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        lineHeightStyle = tightLineHeight,
    ),
    displaySmall = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        lineHeightStyle = tightLineHeight,
    ),
    headlineLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)
