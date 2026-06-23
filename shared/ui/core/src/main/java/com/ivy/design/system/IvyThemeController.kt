package com.ivy.design.system

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ivy.design.system.colors.ColorShades
import com.ivy.design.system.colors.IvyColors

/**
 * Accent palettes the user can pick when *not* using system (Material You) dynamic color.
 * Each maps to one of the [IvyColors] families and seeds the primary/secondary tonal roles.
 */
enum class IvyAccent(val shades: ColorShades) {
    Purple(IvyColors.Purple),
    Blue(IvyColors.Blue),
    Green(IvyColors.Green),
    Orange(IvyColors.Orange),
    Pink(IvyColors.Pink),
    Red(IvyColors.Red),
    Yellow(IvyColors.Yellow),
}

/**
 * App-wide appearance preferences backing the Material 3 theme.
 *
 * Holds Compose [androidx.compose.runtime.MutableState] so that reading these inside
 * [IvyMaterial3Theme] makes the whole app recompose/re-theme the instant the user changes them
 * in Settings. Values are persisted to [android.content.SharedPreferences]; call [ensureLoaded]
 * once at app start (see the Application class).
 */
object IvyThemeController {
    private const val PREFS = "ivy_theme_prefs"
    private const val KEY_DYNAMIC = "use_dynamic_color"
    private const val KEY_ACCENT = "accent"

    /** When true (and on Android 12+), the scheme is generated from the wallpaper (Material You). */
    var useDynamicColor by mutableStateOf(true)
        private set

    /** The accent used when [useDynamicColor] is false (or below Android 12). */
    var accent by mutableStateOf(IvyAccent.Purple)
        private set

    @Volatile
    private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        useDynamicColor = prefs.getBoolean(KEY_DYNAMIC, true)
        accent = runCatching {
            IvyAccent.valueOf(prefs.getString(KEY_ACCENT, IvyAccent.Purple.name)!!)
        }.getOrDefault(IvyAccent.Purple)
        loaded = true
    }

    fun setUseDynamicColor(context: Context, value: Boolean) {
        useDynamicColor = value
        persist(context)
    }

    fun setAccent(context: Context, value: IvyAccent) {
        accent = value
        persist(context)
    }

    private fun persist(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DYNAMIC, useDynamicColor)
            .putString(KEY_ACCENT, accent.name)
            .apply()
    }
}
