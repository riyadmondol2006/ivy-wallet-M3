package com.ivy.settings.cloudsync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.ivy.data.sync.PushError
import com.ivy.data.sync.SyncConfigDataSource
import com.ivy.data.sync.SyncEndpointType
import com.ivy.data.sync.SyncMode
import com.ivy.data.sync.SyncRepository
import com.ivy.ui.ComposeViewModel
import com.ivy.ui.sync.SyncMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@HiltViewModel
class CloudSyncViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val configDataSource: SyncConfigDataSource,
) : ComposeViewModel<CloudSyncState, CloudSyncEvent>() {

    private val url = mutableStateOf("")
    private val token = mutableStateOf("")
    private val endpointType = mutableStateOf(SyncEndpointType.HTTPS)
    private val mode = mutableStateOf(SyncMode.OFF)
    private val savedUrl = mutableStateOf<String?>(null)
    private val savedToken = mutableStateOf<String?>(null)
    private val savedEndpointType = mutableStateOf<SyncEndpointType?>(null)
    private val testStatus = mutableStateOf<TestStatus>(TestStatus.Idle)
    private val busy = mutableStateOf(false)
    private val remoteSummary = mutableStateOf<RemoteSummary?>(null)
    private val message = mutableStateOf<SyncMessage?>(null)
    private val overwriteOffered = mutableStateOf(false)
    private val launchedFromOnboarding = mutableStateOf(false)
    private val onboardingRestore = mutableStateOf<OnboardingRestoreUi>(OnboardingRestoreUi.Hidden)
    private val completion = mutableStateOf(CompletionSignal.NONE)

    fun setLaunchedFromOnboarding(value: Boolean) {
        launchedFromOnboarding.value = value
    }

    @Composable
    override fun uiState(): CloudSyncState {
        LaunchedEffect(Unit) { onStart() }

        val currentUrl = url.value.trim()
        val currentToken = token.value.trim()
        val savedConfigured = !savedUrl.value.isNullOrBlank() && !savedToken.value.isNullOrBlank()
        val changed = currentUrl != savedUrl.value.orEmpty() ||
            currentToken != savedToken.value.orEmpty() ||
            endpointType.value != savedEndpointType.value
        val canSave = testStatus.value is TestStatus.Success &&
            currentUrl.isNotBlank() && currentToken.isNotBlank() &&
            (changed || !savedConfigured)

        return CloudSyncState(
            url = url.value,
            token = token.value,
            endpointType = endpointType.value,
            mode = mode.value,
            savedConfigured = savedConfigured,
            testStatus = testStatus.value,
            canSave = canSave,
            busy = busy.value,
            remoteSummary = remoteSummary.value,
            message = message.value,
            overwriteOffered = overwriteOffered.value,
            launchedFromOnboarding = launchedFromOnboarding.value,
            onboardingRestore = onboardingRestore.value,
            completion = completion.value,
        )
    }

    private suspend fun onStart() {
        val config = configDataSource.get()
        url.value = config.endpointUrl.orEmpty()
        token.value = config.token.orEmpty()
        endpointType.value = config.endpointType
        mode.value = config.mode
        savedUrl.value = config.endpointUrl
        savedToken.value = config.token
        savedEndpointType.value = if (config.isConfigured) config.endpointType else null
        if (config.isConfigured) {
            // Already validated when it was saved.
            testStatus.value = TestStatus.Success
            refreshRemoteSummary()
        }
    }

    override fun onEvent(event: CloudSyncEvent) {
        when (event) {
            is CloudSyncEvent.UrlChanged -> {
                url.value = event.url
                invalidateTest()
            }

            is CloudSyncEvent.TokenChanged -> {
                token.value = event.token
                invalidateTest()
            }

            is CloudSyncEvent.SetEndpointType -> {
                endpointType.value = event.type
                invalidateTest()
            }

            CloudSyncEvent.TestConnection -> testConnection()
            CloudSyncEvent.Save -> save()
            is CloudSyncEvent.SetMode -> setMode(event.mode)
            CloudSyncEvent.SyncNow -> syncNow(force = false)
            CloudSyncEvent.ForceSyncNow -> syncNow(force = true)
            CloudSyncEvent.RestoreNow -> restoreNow()
            CloudSyncEvent.RemoveConnection -> removeConnection()
            CloudSyncEvent.DismissMessage -> message.value = null
            CloudSyncEvent.OnboardingRestore -> onboardingRestore()
            CloudSyncEvent.OnboardingStartFresh ->
                completion.value = CompletionSignal.RETURN_TO_ONBOARDING

            CloudSyncEvent.ConsumeCompletion -> completion.value = CompletionSignal.NONE
        }
    }

    private fun invalidateTest() {
        if (testStatus.value is TestStatus.Success || testStatus.value is TestStatus.Error) {
            testStatus.value = TestStatus.Idle
        }
    }

    /** Runs [block] with the busy indicator on, resetting it even if the block throws. */
    private fun launchBusy(block: suspend () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            try {
                block()
            } finally {
                busy.value = false
            }
        }
    }

    private fun testConnection(): Unit = launchBusy {
        testStatus.value = TestStatus.Testing
        syncRepository.testConnection(endpointType.value, url.value, token.value).fold(
            ifLeft = { testStatus.value = TestStatus.Error(it) },
            ifRight = { testStatus.value = TestStatus.Success },
        )
    }

    private fun save(): Unit = launchBusy {
        val cleanUrl = url.value.trim().trimEnd('/')
        val cleanToken = token.value.trim()
        configDataSource.setConnection(cleanUrl, cleanToken, endpointType.value)
        savedUrl.value = cleanUrl
        savedToken.value = cleanToken
        savedEndpointType.value = endpointType.value
        if (mode.value == SyncMode.OFF) {
            mode.value = SyncMode.MANUAL
            configDataSource.setMode(SyncMode.MANUAL)
        }
        message.value = SyncMessage.ConnectionSaved

        if (launchedFromOnboarding.value) {
            onboardingRestore.value = OnboardingRestoreUi.Checking
            val status = syncRepository.checkRemote()
            val meta = status.meta
            onboardingRestore.value = if (status.exists && meta != null) {
                OnboardingRestoreUi.BackupFound(meta.accounts, meta.updatedAt)
            } else {
                OnboardingRestoreUi.NoBackup
            }
        } else {
            refreshRemoteSummary()
        }
    }

    private fun setMode(newMode: SyncMode) {
        val previous = mode.value
        mode.value = newMode
        viewModelScope.launch {
            configDataSource.setMode(newMode)
            // Switching to AUTO should get the cloud up to date right away, not on the next edit.
            if (newMode == SyncMode.AUTO && previous != SyncMode.AUTO) {
                syncNow(force = false)
            }
        }
    }

    private fun syncNow(force: Boolean): Unit = launchBusy {
        overwriteOffered.value = false
        syncRepository.push(force = force).fold(
            ifLeft = { error ->
                when (error) {
                    is PushError.RemoteNewer -> {
                        message.value = SyncMessage.RemoteNewer
                        overwriteOffered.value = true
                    }

                    is PushError.Failed -> message.value = SyncMessage.Failed(error.message)
                }
            },
            ifRight = {
                message.value = SyncMessage.BackedUp
                refreshRemoteSummary()
            },
        )
    }

    private fun restoreNow(): Unit = launchBusy {
        overwriteOffered.value = false
        syncRepository.pull().fold(
            ifLeft = { message.value = SyncMessage.Failed(it) },
            ifRight = { result ->
                message.value = SyncMessage.Restored(result.transactionsImported)
                refreshRemoteSummary()
            },
        )
    }

    private fun onboardingRestore(): Unit = launchBusy {
        syncRepository.pull().fold(
            ifLeft = { message.value = SyncMessage.Failed(it) },
            ifRight = { completion.value = CompletionSignal.FINISH_ONBOARDING },
        )
    }

    private fun removeConnection() {
        viewModelScope.launch {
            configDataSource.clearConnection()
            savedUrl.value = null
            savedToken.value = null
            savedEndpointType.value = null
            url.value = ""
            token.value = ""
            endpointType.value = SyncEndpointType.HTTPS
            mode.value = SyncMode.OFF
            testStatus.value = TestStatus.Idle
            remoteSummary.value = null
            overwriteOffered.value = false
            message.value = SyncMessage.ConnectionRemoved
        }
    }

    private suspend fun refreshRemoteSummary() {
        val status = syncRepository.checkRemote()
        remoteSummary.value = status.meta?.let { meta ->
            RemoteSummary(
                updatedAtMillis = meta.updatedAt,
                accounts = meta.accounts,
                fromThisDevice = !status.isFromOtherDevice,
            )
        }
    }
}
