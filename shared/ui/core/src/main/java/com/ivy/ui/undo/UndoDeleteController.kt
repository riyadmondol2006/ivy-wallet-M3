package com.ivy.ui.undo

import androidx.compose.material3.SnackbarResult
import com.ivy.base.di.AppCoroutineScope
import com.ivy.base.resource.ResourceProvider
import com.ivy.ui.R
import com.ivy.ui.snackbar.IvySnackbarController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offers an "Undo" snackbar after a delete. The item is already gone when the snackbar shows; if
 * the user taps Undo, [restore] writes it back. Runs on the application scope because the screen
 * (and its ViewModel) that deleted the item usually closes right away.
 */
@Singleton
class UndoDeleteController @Inject constructor(
    private val snackbar: IvySnackbarController,
    private val resourceProvider: ResourceProvider,
    @AppCoroutineScope private val scope: CoroutineScope,
) {
    fun offer(message: String, restore: suspend () -> Unit) {
        scope.launch {
            val result = snackbar.show(
                message = message,
                actionLabel = resourceProvider.getString(R.string.undo),
            )
            if (result == SnackbarResult.ActionPerformed) {
                try {
                    restore()
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    snackbar.show(resourceProvider.getString(R.string.undo_failed))
                }
            }
        }
    }
}
