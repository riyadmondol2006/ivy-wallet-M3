package com.ivy.ui.snackbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.design.system.IvySpacing
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide snackbar queue. ViewModels inject it and call [show]; the single [IvySnackbarHost]
 * mounted in the root activity renders the messages above whatever screen is visible.
 */
@Singleton
class IvySnackbarController @Inject constructor() {
    val hostState = SnackbarHostState()

    /**
     * Extra space to keep above the bottom edge, e.g. the height of the Home bottom bar, so the
     * snackbar never covers it. Screens that own a bottom bar set it while they are visible.
     */
    var bottomOffset: Dp by mutableStateOf(0.dp)

    suspend fun show(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = if (actionLabel != null) {
            SnackbarDuration.Long
        } else {
            SnackbarDuration.Short
        },
    ): SnackbarResult = hostState.showSnackbar(
        message = message,
        actionLabel = actionLabel,
        withDismissAction = actionLabel == null,
        duration = duration,
    )
}

/** Renders the app-wide snackbar queue; mount once, above the navigation host. */
@Composable
fun IvySnackbarHost(
    controller: IvySnackbarController,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = controller.hostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = IvySpacing.screenGutter)
                .padding(bottom = controller.bottomOffset),
        ) { data ->
            Snackbar(
                snackbarData = data,
                shape = MaterialTheme.shapes.medium,
                actionColor = MaterialTheme.colorScheme.inversePrimary,
            )
        }
    }
}
