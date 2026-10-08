package com.ivy.settings.cloudsync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.ivy.data.sync.PushError
import com.ivy.data.sync.RedisConnectionString
import com.ivy.data.sync.RemoteStatus
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
    private val quickAddStatus = mutableStateOf<QuickAddStatus>(QuickAddStatus.Idle)
    private val existingBackup = mutableStateOf<ExistingBackup?>(null)

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
            quickAdd = quickAddStatus.value,
            existingBackup = existingBackup.value,
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
            is CloudSyncEvent.UrlChanged -> onUrlChanged(event.url)

            is CloudSyncEvent.TokenChanged -> {
                token.value = event.token
                invalidateTest()
            }

            is CloudSyncEvent.SetEndpointType -> {
                endpointType.value = event.type
                invalidateTest()
            }

            is CloudSyncEvent.QuickAdd -> quickAdd(event.connection)
            CloudSyncEvent.TestConnection -> testConnection()
            CloudSyncEvent.Save -> save()
            is CloudSyncEvent.SetMode -> setMode(event.mode)
            CloudSyncEvent.SyncNow -> syncNow(force = false)
            CloudSyncEvent.ForceSyncNow -> syncNow(force = true)
            CloudSyncEvent.RestoreNow -> restoreNow()
            CloudSyncEvent.RestoreExisting -> {
                existingBackup.value = null
                restoreNow()
            }

            CloudSyncEvent.OverwriteExisting -> {
                existingBackup.value = null
                syncNow(force = true)
            }

            CloudSyncEvent.DismissExisting -> existingBackup.value = null
            CloudSyncEvent.RemoveConnection -> removeConnection()
            CloudSyncEvent.DismissMessage -> message.value = null
            CloudSyncEvent.OnboardingRestore -> onboardingRestore()
            CloudSyncEvent.OnboardingStartFresh ->
                completion.value = CompletionSignal.RETURN_TO_ONBOARDING

            CloudSyncEvent.ConsumeCompletion -> completion.value = CompletionSignal.NONE
        }
    }

    /**
     * A connection string pasted from the Upstash console (`redis-cli --tls -u redis://…` or a
     * `redis(s)://user:password@host` URL) holds everything needed: switch to TCP, which works
     * with any Redis, and split it into the URL and password fields. Only for a paste, so the
     * field is never rewritten while someone types a URL by hand.
     */
    private fun onUrlChanged(input: String) {
        val isPaste = input.length - url.value.length > 1
        if (isPaste && isConnectionLine(input)) {
            // A complete connection line was pasted: set it up right away, like Quick add.
            quickAdd(input)
        } else {
            url.value = input
            invalidateTest()
        }
    }

    companion object {
        /** True for a pasted connection string that carries its password. */
        fun isConnectionLine(text: String): Boolean =
            RedisConnectionString.isRedisConnectionString(text) &&
                RedisConnectionString.parse(text)?.password != null
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

    /**
     * Sets the database up from the one line the Upstash console shows
     * (`redis-cli --tls -u redis://default:PASSWORD@host:6379`): fills in the TCP connection,
     * tests it and saves it, replacing any database saved before.
     */
    private fun quickAdd(connection: String): Unit = launchBusy {
        val address = RedisConnectionString.parse(connection)?.takeIf { it.password != null }
        if (address == null) {
            quickAddStatus.value = QuickAddStatus.InvalidInput
            return@launchBusy
        }
        endpointType.value = SyncEndpointType.TCP
        url.value = address.tlsUrl
        token.value = address.password.orEmpty()
        quickAddStatus.value = QuickAddStatus.Connecting
        testStatus.value = TestStatus.Testing
        syncRepository.testConnection(endpointType.value, url.value, token.value).fold(
            ifLeft = {
                testStatus.value = TestStatus.Error(it)
                quickAddStatus.value = QuickAddStatus.Failed(it)
            },
            ifRight = {
                testStatus.value = TestStatus.Success
                saveConnection()
                quickAddStatus.value = QuickAddStatus.Connected
            },
        )
    }

    private fun save(): Unit = launchBusy { saveConnection() }

    private suspend fun saveConnection() {
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
            val status = refreshRemoteSummary()
            // Ask what to do with a backup that is already there, as onboarding does.
            existingBackup.value = status.meta?.takeIf { status.exists }?.let { meta ->
                ExistingBackup(accounts = meta.accounts, updatedAtMillis = meta.updatedAt)
            }
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

    private suspend fun refreshRemoteSummary(): RemoteStatus {
        val status = syncRepository.checkRemote()
        remoteSummary.value = status.meta?.let { meta ->
            RemoteSummary(
                updatedAtMillis = meta.updatedAt,
                accounts = meta.accounts,
                fromThisDevice = !status.isFromOtherDevice,
            )
        }
        return status
    }
}
