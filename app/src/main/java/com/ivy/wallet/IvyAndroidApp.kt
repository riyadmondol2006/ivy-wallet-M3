package com.ivy.wallet

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ivy.base.legacy.appContext
import com.ivy.design.system.IvyThemeController
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import timber.log.Timber.DebugTree
import javax.inject.Inject

/**
 * Created by iliyan on 24.02.18.
 */
@HiltAndroidApp
class IvyAndroidApp : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        appContext = this
        // Load saved appearance prefs (dynamic color + accent) before any UI composes.
        IvyThemeController.ensureLoaded(this)

        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
    }
}
