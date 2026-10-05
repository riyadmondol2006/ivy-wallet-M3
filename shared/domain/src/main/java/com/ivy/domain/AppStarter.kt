package com.ivy.domain

import android.content.Intent
import com.ivy.base.model.TransactionType

/**
 * A component used to start the **RootActivity** without knowing about it.
 */
interface AppStarter {
    fun getRootIntent(): Intent
    fun defaultStart()
    fun addTransactionStart(type: TransactionType)

    /** Intent that opens the app on the Planned payments screen (used by reminder notifications). */
    fun openPlannedPaymentsIntent(): Intent

    companion object {
        /** String extra naming a screen the app should open on start; see [SCREEN_PLANNED_PAYMENTS]. */
        const val EXTRA_OPEN_SCREEN = "open_screen_extra"
        const val SCREEN_PLANNED_PAYMENTS = "planned_payments"
    }
}
