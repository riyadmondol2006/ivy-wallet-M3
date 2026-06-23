package com.ivy.importdata.csvimport.flow

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.data.backup.ImportResult
import com.ivy.importdata.csv.Spacer8
import com.ivy.legacy.utils.format
import com.ivy.navigation.CSVScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.BackButton
import kotlinx.collections.immutable.persistentListOf

@SuppressLint("ComposeModifierMissing")
@Composable
fun ImportResultUI(
    result: ImportResult,
    launchedFromOnboarding: Boolean,
    isManualCsvImport: Boolean = false,
    onTryAgain: (() -> Unit)? = null,
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Spacer(Modifier.height(16.dp))

        val nav = navigation()
        BackButton(
            modifier = Modifier.padding(start = 20.dp)
        ) {
            nav.onBackPressed()
        }

        Spacer(Modifier.height(24.dp))

        val importSuccess = result.transactionsImported > 0 &&
                result.transactionsImported > result.rowsFound / 2
        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = if (importSuccess) stringResource(R.string.success) else stringResource(R.string.failure),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = if (importSuccess) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.error
            }
        )

        Spacer(Modifier.height(32.dp))

        val successPercent = if (result.rowsFound > 0) {
            (result.transactionsImported / result.rowsFound.toDouble()) * 100
        } else {
            0.0
        }

        SuccessSectionUI(
            result = result,
            successPercent = successPercent,
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        FailedSectionUI(
            result = result,
            successPercent = successPercent,
        )

        // TODO: Implement "See failed imports"

        if (!isManualCsvImport) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                text = stringResource(R.string.csv_import_failed),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 16.dp),
                onClick = {
                    nav.navigateTo(CSVScreen(launchedFromOnboarding = launchedFromOnboarding))
                }
            ) {
                Text(
                    text = stringResource(id = R.string.manual_csv_import),
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Spacer8()

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 16.dp),
            onClick = onFinish,
        ) {
            Text(
                text = stringResource(R.string.finish),
                fontWeight = FontWeight.Bold,
            )
        }

        if (onTryAgain != null) {
            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                onClick = onTryAgain,
                enabled = true
            ) {
                Text(text = stringResource(R.string.try_again))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SuccessSectionUI(
    result: ImportResult,
    successPercent: Double,
) {
    Column {
        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.imported),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.tertiary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = "${successPercent.format(2)}%",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.transactions_imported, result.transactionsImported),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(4.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.accounts_imported, result.accountsImported),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(4.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.categories_imported, result.categoriesImported),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun FailedSectionUI(
    result: ImportResult,
    successPercent: Double,
) {
    Column {
        Spacer(Modifier.height(32.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.failed),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(Modifier.height(8.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = "${(100 - successPercent).format(2)}%",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(
                R.string.rows_from_csv_not_recognized,
                result.rowsFound - result.transactionsImported
            ),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(device = "id:pixel_3", showBackground = true, showSystemUi = true)
@Composable
private fun Preview() {
    com.ivy.legacy.IvyWalletPreview {
        ImportResultUI(
            result = ImportResult(
                rowsFound = 356,
                transactionsImported = 320,
                accountsImported = 4,
                categoriesImported = 13,
                failedRows = persistentListOf()
            ),
            launchedFromOnboarding = false,
        ) {
        }
    }
}
