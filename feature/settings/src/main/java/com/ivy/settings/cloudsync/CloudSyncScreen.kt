package com.ivy.settings.cloudsync

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.data.sync.SyncEndpointType
import com.ivy.data.sync.SyncMode
import com.ivy.legacy.rootScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.onboarding.OnboardingEvent
import com.ivy.onboarding.viewmodel.OnboardingViewModel
import com.ivy.ui.R
import java.text.DateFormat
import java.util.Date

private const val UpstashConsoleUrl = "https://console.upstash.com"

@Composable
fun BoxWithConstraintsScope.CloudSyncScreen(screen: com.ivy.navigation.CloudSyncScreen) {
    val viewModel: CloudSyncViewModel = screenScopedViewModel()
    LaunchedEffect(screen.launchedFromOnboarding) {
        viewModel.setLaunchedFromOnboarding(screen.launchedFromOnboarding)
    }
    val state = viewModel.uiState()
    val onboardingViewModel: OnboardingViewModel? =
        if (screen.launchedFromOnboarding) viewModel() else null
    val nav = navigation()
    val rootScreen = rootScreen()
    val context = LocalContext.current

    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onEvent(CloudSyncEvent.DismissMessage)
        }
    }

    LaunchedEffect(state.completion) {
        when (state.completion) {
            CompletionSignal.FINISH_ONBOARDING -> {
                onboardingViewModel?.cloudRestoreFinished()
                viewModel.onEvent(CloudSyncEvent.ConsumeCompletion)
            }

            CompletionSignal.RETURN_TO_ONBOARDING -> {
                onboardingViewModel?.onEvent(OnboardingEvent.StartFresh)
                nav.back()
                viewModel.onEvent(CloudSyncEvent.ConsumeCompletion)
            }

            CompletionSignal.NONE -> Unit
        }
    }

    CloudSyncUi(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = { nav.back() },
        onOpenUpstash = { rootScreen.openUrlInBrowser(UpstashConsoleUrl) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
private fun CloudSyncUi(
    state: CloudSyncState,
    onEvent: (CloudSyncEvent) -> Unit,
    onBack: () -> Unit,
    onOpenUpstash: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.cloud_sync_setup_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        if (state.busy) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(16.dp))
        GuideCard(onOpenUpstash = onOpenUpstash)

        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.cloud_sync_endpoint_type),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SyncEndpointType.entries.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = state.endpointType == type,
                    onClick = { onEvent(CloudSyncEvent.SetEndpointType(type)) },
                    shape = SegmentedButtonDefaults.itemShape(index, SyncEndpointType.entries.size),
                ) {
                    Text(stringResource(type.endpointLabelRes()))
                }
            }
        }

        val isTcp = state.endpointType == SyncEndpointType.TCP
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.url,
            onValueChange = { onEvent(CloudSyncEvent.UrlChanged(it)) },
            label = {
                Text(
                    stringResource(
                        if (isTcp) R.string.cloud_sync_tcp_url_label else R.string.cloud_sync_url_label
                    )
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.token,
            onValueChange = { onEvent(CloudSyncEvent.TokenChanged(it)) },
            label = {
                Text(
                    stringResource(
                        if (isTcp) {
                            R.string.cloud_sync_tcp_token_label
                        } else {
                            R.string.cloud_sync_token_label
                        }
                    )
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))
        TestRow(state = state, onEvent = onEvent)

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { onEvent(CloudSyncEvent.Save) },
            enabled = state.canSave && !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.cloud_sync_save))
        }
        if (!state.canSave && state.testStatus !is TestStatus.Success) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.cloud_sync_test_first),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.launchedFromOnboarding) {
            OnboardingRestoreSection(state = state, onEvent = onEvent)
        } else if (state.savedConfigured) {
            ConfiguredSection(state = state, onEvent = onEvent)
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun GuideCard(onOpenUpstash: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.cloud_sync_guide_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            listOf(
                R.string.cloud_sync_guide_step_1,
                R.string.cloud_sync_guide_step_2,
                R.string.cloud_sync_guide_step_3,
                R.string.cloud_sync_guide_step_4,
            ).forEach { step ->
                Text(
                    text = stringResource(step),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onOpenUpstash) {
                Text(stringResource(R.string.cloud_sync_open_upstash))
            }
        }
    }
}

@Composable
private fun TestRow(state: CloudSyncState, onEvent: (CloudSyncEvent) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            onClick = { onEvent(CloudSyncEvent.TestConnection) },
            enabled = state.url.isNotBlank() && state.token.isNotBlank() && !state.busy,
        ) {
            Text(stringResource(R.string.cloud_sync_test_connection))
        }
        Spacer(Modifier.width(12.dp))
        when (val test = state.testStatus) {
            is TestStatus.Testing -> {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }

            is TestStatus.Success -> Text(
                text = stringResource(R.string.cloud_sync_test_success),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            is TestStatus.Error -> Text(
                text = test.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )

            is TestStatus.Idle -> Unit
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.ConfiguredSection(state: CloudSyncState, onEvent: (CloudSyncEvent) -> Unit) {
    Spacer(Modifier.height(24.dp))
    HorizontalDivider()
    Spacer(Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.cloud_sync_mode_label),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(8.dp))
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SyncMode.entries.forEachIndexed { index, syncMode ->
            SegmentedButton(
                selected = state.mode == syncMode,
                onClick = { onEvent(CloudSyncEvent.SetMode(syncMode)) },
                shape = SegmentedButtonDefaults.itemShape(index, SyncMode.entries.size),
            ) {
                Text(stringResource(syncMode.labelRes()))
            }
        }
    }
    val hint = when (state.mode) {
        SyncMode.MANUAL -> R.string.cloud_sync_mode_manual_hint
        SyncMode.AUTO -> R.string.cloud_sync_mode_auto_hint
        SyncMode.OFF -> null
    }
    if (hint != null) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Spacer(Modifier.height(16.dp))
    RemoteSummaryText(summary = state.remoteSummary)

    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = { onEvent(CloudSyncEvent.SyncNow) },
            enabled = !state.busy,
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.cloud_sync_now))
        }
        OutlinedButton(
            onClick = { onEvent(CloudSyncEvent.RestoreNow) },
            enabled = !state.busy,
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.cloud_sync_restore))
        }
    }

    Spacer(Modifier.height(8.dp))
    TextButton(
        onClick = { onEvent(CloudSyncEvent.RemoveConnection) },
        enabled = !state.busy,
    ) {
        Text(
            text = stringResource(R.string.cloud_sync_remove),
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun ColumnScope.OnboardingRestoreSection(
    state: CloudSyncState,
    onEvent: (CloudSyncEvent) -> Unit,
) {
    when (val restore = state.onboardingRestore) {
        OnboardingRestoreUi.Hidden -> Unit
        OnboardingRestoreUi.Checking -> {
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator()
        }

        is OnboardingRestoreUi.BackupFound -> {
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.cloud_sync_restore_found_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.cloud_sync_restore_found_desc,
                    restore.accounts,
                    formatTime(restore.updatedAtMillis),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onEvent(CloudSyncEvent.OnboardingRestore) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.cloud_sync_restore_sync_from_it))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onEvent(CloudSyncEvent.OnboardingStartFresh) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.cloud_sync_start_fresh))
            }
        }

        OnboardingRestoreUi.NoBackup -> {
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.cloud_sync_no_backup_found),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onEvent(CloudSyncEvent.OnboardingStartFresh) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.cloud_sync_continue))
            }
        }
    }
}

@Composable
private fun RemoteSummaryText(summary: RemoteSummary?) {
    if (summary == null) {
        Text(
            text = stringResource(R.string.cloud_sync_no_backup_yet),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val origin = if (summary.fromThisDevice) {
        stringResource(R.string.cloud_sync_from_this_device)
    } else {
        stringResource(R.string.cloud_sync_from_other_device)
    }
    Text(
        text = stringResource(R.string.cloud_sync_last_backup, formatTime(summary.updatedAtMillis)) +
            " · " + stringResource(R.string.cloud_sync_accounts_count, summary.accounts) +
            " · " + origin,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun SyncMode.labelRes(): Int = when (this) {
    SyncMode.OFF -> R.string.cloud_sync_mode_off
    SyncMode.MANUAL -> R.string.cloud_sync_mode_manual
    SyncMode.AUTO -> R.string.cloud_sync_mode_auto
}

private fun SyncEndpointType.endpointLabelRes(): Int = when (this) {
    SyncEndpointType.HTTPS -> R.string.cloud_sync_endpoint_https
    SyncEndpointType.TCP -> R.string.cloud_sync_endpoint_tcp
}

private fun formatTime(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
