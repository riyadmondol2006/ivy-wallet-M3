package com.ivy.wallet.update

import com.ivy.base.di.AppCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the update prompt for the app process. It lives outside `RootViewModel` on purpose:
 * `NavigationRoot` clears the activity's ViewModel store when leaving a screen, which would cancel
 * an in-flight check (e.g. while the user accepts the disclaimer on first launch).
 */
@Singleton
class AppUpdateController @Inject constructor(
    private val checker: AppUpdateChecker,
    @AppCoroutineScope private val scope: CoroutineScope,
) {
    private val checkStarted = AtomicBoolean(false)

    private val _update = MutableStateFlow<AppUpdate?>(null)

    /** A newer GitHub release to offer, or null. */
    val update: StateFlow<AppUpdate?> = _update.asStateFlow()

    /** Checks GitHub once per app process; later calls (e.g. activity recreation) do nothing. */
    fun checkOnce() {
        if (!checkStarted.compareAndSet(false, true)) return
        scope.launch {
            _update.value = checker.findUpdate()
        }
    }

    /** Hides the prompt until the next app start. */
    fun dismiss() {
        _update.value = null
    }

    /** Never offers this version again; a newer release will still be offered. */
    fun skip(update: AppUpdate) {
        _update.value = null
        scope.launch {
            checker.skipVersion(update.version)
        }
    }
}
