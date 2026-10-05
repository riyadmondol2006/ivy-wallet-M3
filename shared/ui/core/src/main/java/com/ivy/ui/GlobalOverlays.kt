package com.ivy.ui

import androidx.compose.runtime.Composable
import com.ivy.design.system.IvyMaterial3Theme
import com.ivy.ui.snackbar.IvySnackbarController
import com.ivy.ui.snackbar.IvySnackbarHost
import com.ivy.ui.time.impl.DateTimePicker

/**
 * Pickers and snackbars that must draw above every screen, themed like the app. Mounted once by
 * the root activity after the navigation host.
 */
@Composable
fun GlobalOverlays(
    dark: Boolean,
    isTrueBlack: Boolean,
    dateTimePicker: DateTimePicker,
    snackbarController: IvySnackbarController,
) {
    IvyMaterial3Theme(dark = dark, isTrueBlack = isTrueBlack) {
        dateTimePicker.Content()
        IvySnackbarHost(controller = snackbarController)
    }
}
