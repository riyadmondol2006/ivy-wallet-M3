package com.ivy.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.l1_buildingBlocks.IconScale
import com.ivy.design.l1_buildingBlocks.IvyIconScaled
import com.ivy.design.utils.thenIf
import com.ivy.legacy.Constants
import com.ivy.legacy.IvyWalletPreview
import com.ivy.data.sync.SyncMode
import com.ivy.legacy.rootScreen
import com.ivy.navigation.CloudSyncScreen
import com.ivy.navigation.ExchangeRatesScreen
import com.ivy.navigation.FeaturesScreen
import com.ivy.navigation.ImportScreen
import com.ivy.navigation.ReleasesScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.ui.theme.Gradient
import com.ivy.wallet.ui.theme.MediumBlack
import com.ivy.wallet.ui.theme.Red
import com.ivy.wallet.ui.theme.Red3
import com.ivy.wallet.ui.theme.White
import com.ivy.wallet.ui.theme.components.IvyToolbar
import com.ivy.wallet.ui.theme.modal.ChooseStartDateOfMonthModal
import com.ivy.wallet.ui.theme.modal.CurrencyModal
import com.ivy.wallet.ui.theme.modal.DeleteModal
import com.ivy.wallet.ui.theme.modal.NameModal
import com.ivy.wallet.ui.theme.modal.ProgressModal
import java.util.Locale

@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.SettingsScreen() {
    val viewModel: SettingsViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()
    val rootScreen = rootScreen()

    UI(
        currencyCode = uiState.currencyCode,
        theme = uiState.currentTheme,
        onSwitchTheme = {
            viewModel.onEvent(SettingsEvent.SwitchTheme)
        },
        lockApp = uiState.lockApp,
        showNotifications = uiState.showNotifications,
        hideCurrentBalance = uiState.hideCurrentBalance,
        hideIncome = uiState.hideIncome,
        progressState = uiState.progressState,
        treatTransfersAsIncomeExpense = uiState.treatTransfersAsIncomeExpense,
        creditCardsEnabled = uiState.creditCardsEnabled,
        nameLocalAccount = uiState.name,
        startDateOfMonth = uiState.startDateOfMonth.toInt(),
        languageOptionVisible = uiState.languageOptionVisible,
        cloudSyncMode = uiState.cloudSyncMode,
        onSetCurrency = {
            viewModel.onEvent(SettingsEvent.SetCurrency(it))
        },
        onSetName = {
            viewModel.onEvent(SettingsEvent.SetName(it))
        },
        onBackupData = {
            viewModel.onEvent(SettingsEvent.BackupData(rootScreen))
        },
        onExportToCSV = {
            viewModel.onEvent(SettingsEvent.ExportToCsv(rootScreen))
        },
        onSetLockApp = {
            viewModel.onEvent(SettingsEvent.SetLockApp(it))
        },
        onSetShowNotifications = {
            viewModel.onEvent(SettingsEvent.SetShowNotifications(it))
        },
        onSetHideCurrentBalance = {
            viewModel.onEvent(SettingsEvent.SetHideCurrentBalance(it))
        },
        onSetHideIncome = {
            viewModel.onEvent(SettingsEvent.SetHideIncome(it))
        },
        onSetStartDateOfMonth = {
            viewModel.onEvent(SettingsEvent.SetStartDateOfMonth(it))
        },
        onSetTreatTransfersAsIncExp = {
            viewModel.onEvent(SettingsEvent.SetTransfersAsIncomeExpense(it))
        },
        onSetCreditCards = {
            viewModel.onEvent(SettingsEvent.SetCreditCardsEnabled(it))
        },
        onDeleteAllUserData = {
            viewModel.onEvent(SettingsEvent.DeleteAllUserData)
        },
        onDeleteCloudUserData = {
            viewModel.onEvent(SettingsEvent.DeleteCloudUserData)
        },
        onSwitchLanguage = {
            viewModel.onEvent(SettingsEvent.SwitchLanguage)
        }
    )
}

@ExperimentalFoundationApi
@Composable
@Suppress("LongMethod")
private fun BoxWithConstraintsScope.UI(
    currencyCode: String,
    theme: Theme,
    onSwitchTheme: () -> Unit,
    lockApp: Boolean,
    nameLocalAccount: String?,
    languageOptionVisible: Boolean,
    onSetCurrency: (String) -> Unit,
    cloudSyncMode: SyncMode = SyncMode.OFF,
    startDateOfMonth: Int = 1,
    showNotifications: Boolean = true,
    hideCurrentBalance: Boolean = false,
    hideIncome: Boolean = false,
    progressState: Boolean = false,
    treatTransfersAsIncomeExpense: Boolean = false,
    creditCardsEnabled: Boolean = false,
    onSetName: (String) -> Unit = {},
    onBackupData: () -> Unit = {},
    onExportToCSV: () -> Unit = {},
    onSetLockApp: (Boolean) -> Unit = {},
    onSetShowNotifications: (Boolean) -> Unit = {},
    onSetTreatTransfersAsIncExp: (Boolean) -> Unit = {},
    onSetCreditCards: (Boolean) -> Unit = {},
    onSetHideCurrentBalance: (Boolean) -> Unit = {},
    onSetHideIncome: (Boolean) -> Unit = {},
    onSetStartDateOfMonth: (Int) -> Unit = {},
    onDeleteAllUserData: () -> Unit = {},
    onDeleteCloudUserData: () -> Unit = {},
    onSwitchLanguage: () -> Unit = {}
) {
    var currencyModalVisible by remember { mutableStateOf(false) }
    var nameModalVisible by remember { mutableStateOf(false) }
    var chooseStartDateOfMonthVisible by remember { mutableStateOf(false) }
    var deleteCloudDataModalVisible by remember { mutableStateOf(false) }
    var deleteAllDataModalVisible by remember { mutableStateOf(false) }
    var deleteAllDataModalFinalVisible by remember { mutableStateOf(false) }
    val nav = navigation()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("settings_lazy_column")
    ) {
        stickyHeader {
            IvyToolbar(
                onBack = { nav.onBackPressed() },
            ) {
                Spacer(Modifier.weight(1f))

                val rootScreen = rootScreen()
                Text(
                    modifier = Modifier.clickable {
                        nav.navigateTo(ReleasesScreen)
                    },
                    text = "${rootScreen.buildVersionName} (${rootScreen.buildVersionCode})",
                    style = UI.typo.nC.style(
                        color = UI.colors.gray,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(Modifier.width(32.dp))
            }
            // onboarding toolbar include paddingBottom 16.dp
        }

        item {
            Spacer(Modifier.height(8.dp))

            Text(
                modifier = Modifier.padding(start = 32.dp),
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(24.dp))

            CurrencyButton(currency = currencyCode) {
                currencyModalVisible = true
            }

            Spacer(Modifier.height(12.dp))

            AccountCard(
                nameLocalAccount = nameLocalAccount,
            ) {
                nameModalVisible = true
            }

//            Spacer(Modifier.height(20.dp))
//            Premium()
        }

        item {
            SettingsSectionDivider(text = stringResource(R.string.cloud_sync))

            Spacer(Modifier.height(16.dp))

            val nav = navigation()
            SettingsDefaultButton(
                icon = R.drawable.ic_vue_security_shield,
                text = stringResource(R.string.cloud_sync),
                description = when (cloudSyncMode) {
                    SyncMode.AUTO -> stringResource(R.string.cloud_sync_mode_auto)
                    SyncMode.MANUAL -> stringResource(R.string.cloud_sync_mode_manual)
                    SyncMode.OFF -> stringResource(R.string.cloud_sync_not_set_up)
                },
                iconPadding = 8.dp
            ) {
                nav.navigateTo(CloudSyncScreen(launchedFromOnboarding = false))
            }
        }

        item {
            SettingsSectionDivider(text = stringResource(R.string.import_export))

            Spacer(Modifier.height(16.dp))

            val nav = navigation()
            ExportCSV {
                onExportToCSV()
            }

            Spacer(Modifier.height(12.dp))

            SettingsDefaultButton(
                icon = R.drawable.ic_vue_security_shield,
                text = stringResource(R.string.backup_data),
                iconPadding = 8.dp
            ) {
                onBackupData()
            }

            Spacer(Modifier.height(12.dp))

            SettingsPrimaryButton(
                icon = R.drawable.ic_export_csv,
                text = stringResource(R.string.import_data),
                backgroundGradient = Gradient.solid(MaterialTheme.colorScheme.primary),
                textColor = MaterialTheme.colorScheme.onPrimary
            ) {
                nav.navigateTo(
                    ImportScreen(
                        launchedFromOnboarding = false
                    )
                )
            }
        }

        item {
            SettingsSectionDivider(text = stringResource(R.string.settings_appearance))

            Spacer(Modifier.height(16.dp))

            AppThemeButton(
                icon = when (theme) {
                    Theme.LIGHT -> R.drawable.home_more_menu_light_mode
                    Theme.DARK -> R.drawable.home_more_menu_dark_mode
                    Theme.AMOLED_DARK -> R.drawable.home_more_menu_amoled_dark_mode
                    Theme.AUTO -> R.drawable.home_more_menu_auto_mode
                },
                label = when (theme) {
                    Theme.LIGHT -> stringResource(R.string.light_mode)
                    Theme.DARK -> stringResource(R.string.dark_mode)
                    Theme.AMOLED_DARK -> stringResource(R.string.amoled_mode)
                    Theme.AUTO -> stringResource(R.string.auto_mode)
                }
            ) {
                onSwitchTheme()
            }

            Spacer(Modifier.height(12.dp))

            // Material You: system color toggle + accent picker.
            com.ivy.design.system.AppearanceCard()
        }

        item {
            SettingsSectionDivider(text = stringResource(R.string.settings_preferences))

            Spacer(Modifier.height(16.dp))

            val nav = navigation()

            if (languageOptionVisible) {
                SettingsDefaultButton(
                    icon = R.drawable.ic_vue_location_global,
                    iconPadding = 6.dp,
                    text = stringResource(R.string.language),
                    description = Locale.getDefault().displayName
                ) {
                    onSwitchLanguage()
                }

                Spacer(Modifier.height(12.dp))
            }

            SettingsDefaultButton(
                icon = R.drawable.ic_currency,
                text = stringResource(R.string.exchange_rates),
            ) {
                nav.navigateTo(ExchangeRatesScreen)
            }

            Spacer(Modifier.height(12.dp))

            StartDateOfMonth(
                startDateOfMonth = startDateOfMonth
            ) {
                chooseStartDateOfMonthVisible = true
            }

            Spacer(Modifier.height(12.dp))

            AppSwitch(
                lockApp = treatTransfersAsIncomeExpense,
                onSetLockApp = onSetTreatTransfersAsIncExp,
                text = stringResource(R.string.transfers_as_income_expense),
                description = stringResource(R.string.transfers_as_income_expense_description),
                icon = R.drawable.ic_custom_transfer_m
            )

            Spacer(Modifier.height(12.dp))

            AppSwitch(
                lockApp = showNotifications,
                onSetLockApp = onSetShowNotifications,
                text = stringResource(R.string.show_notifications),
                icon = R.drawable.ic_notification_m
            )
        }

        item {
            SettingsSectionDivider(text = stringResource(R.string.settings_privacy_security))

            Spacer(Modifier.height(16.dp))

            AppSwitch(
                lockApp = lockApp,
                onSetLockApp = onSetLockApp,
                text = stringResource(R.string.lock_app),
                icon = R.drawable.ic_custom_fingerprint_m
            )

            Spacer(Modifier.height(12.dp))

            AppSwitch(
                lockApp = hideCurrentBalance,
                onSetLockApp = onSetHideCurrentBalance,
                text = stringResource(R.string.hide_balance),
                description = stringResource(R.string.hide_balance_description),
                icon = R.drawable.ic_hide_m
            )

            Spacer(Modifier.height(12.dp))

            AppSwitch(
                lockApp = hideIncome,
                onSetLockApp = onSetHideIncome,
                text = stringResource(R.string.hide_income),
                description = stringResource(R.string.hide_income_description),
                icon = R.drawable.ic_hide_m
            )
        }

        item {
            SettingsSectionDivider(text = stringResource(R.string.settings_features))

            Spacer(Modifier.height(16.dp))

            val nav = navigation()

            AppSwitch(
                lockApp = creditCardsEnabled,
                onSetLockApp = onSetCreditCards,
                text = stringResource(R.string.enable_credit_cards),
                description = stringResource(R.string.enable_credit_cards_description),
                icon = R.drawable.ic_vue_money_card
            )

            Spacer(Modifier.height(12.dp))

            CustomFeatures(
                onClick = { nav.navigateTo(FeaturesScreen) }
            )
        }

//        item {
//            SettingsSectionDivider(text = stringResource(R.string.experimental))
//
//            Spacer(Modifier.height(16.dp))
//
//            val nav = navigation()
//            SettingsDefaultButton(
//                icon = R.drawable.ic_custom_atom_m,
//                text = stringResource(R.string.experimental_settings)
//            ) {
//                nav.navigateTo(ExperimentalScreen)
//            }
//        }

        item {
            SettingsSectionDivider(text = stringResource(R.string.other))

            Spacer(Modifier.height(16.dp))

            val rootScreen = rootScreen()
            SettingsPrimaryButton(
                icon = R.drawable.ic_custom_family_m,
                text = stringResource(R.string.share_ivy_wallet),
                backgroundGradient = Gradient.solid(Red3)
            ) {
                rootScreen.shareIvyWallet()
            }

            Spacer(Modifier.height(12.dp))

            SettingsPrimaryButton(
                icon = R.drawable.github_logo,
                iconPadding = 10.dp,
                text = stringResource(R.string.ivy_wallet_is_opensource),
                backgroundGradient = Gradient.solid(MediumBlack)
            ) {
                rootScreen.openUrlInBrowser(url = Constants.URL_IVY_WALLET_REPO)
            }
        }

        item {
            SettingsSectionDivider(
                text = stringResource(R.string.danger_zone),
                color = Red
            )

            Spacer(Modifier.height(16.dp))

            SettingsPrimaryButton(
                icon = R.drawable.ic_delete,
                text = stringResource(R.string.delete_all_user_data),
                backgroundGradient = Gradient.solid(Red)
            ) {
                deleteAllDataModalVisible = true
            }
        }

        item {
            Spacer(modifier = Modifier.height(120.dp)) // last item spacer
        }
    }

    CurrencyModal(
        title = stringResource(R.string.set_currency),
        initialCurrency = IvyCurrency.fromCode(currencyCode),
        visible = currencyModalVisible,
        dismiss = { currencyModalVisible = false }
    ) {
        onSetCurrency(it)
    }

    NameModal(
        visible = nameModalVisible,
        name = nameLocalAccount ?: "",
        dismiss = { nameModalVisible = false }
    ) {
        onSetName(it)
    }

    ChooseStartDateOfMonthModal(
        visible = chooseStartDateOfMonthVisible,
        selectedStartDateOfMonth = startDateOfMonth,
        dismiss = { chooseStartDateOfMonthVisible = false }
    ) {
        onSetStartDateOfMonth(it)
    }

    DeleteModal(
        title = stringResource(R.string.delete_all_user_data_question),
        description = stringResource(
            R.string.delete_all_user_data_warning,
            stringResource(R.string.your_account)
        ),
        visible = deleteAllDataModalVisible,
        dismiss = { deleteAllDataModalVisible = false },
        onDelete = {
            deleteAllDataModalVisible = false
            deleteAllDataModalFinalVisible = true
        }
    )

    DeleteModal(
        title = stringResource(
            R.string.confirm_all_userd_data_deletion,
            stringResource(R.string.all_of_your_data)
        ),
        description = stringResource(R.string.final_deletion_warning),
        visible = deleteAllDataModalFinalVisible,
        dismiss = { deleteAllDataModalFinalVisible = false },
        onDelete = {
            onDeleteAllUserData()
        }
    )

    DeleteModal(
        title = stringResource(R.string.delete_all_cloud_data_question),
        description = stringResource(
            R.string.delete_all_user_cloud_data_warning,
            stringResource(R.string.your_account)
        ),
        visible = deleteCloudDataModalVisible,
        dismiss = { deleteCloudDataModalVisible = false },
        onDelete = {
            onDeleteCloudUserData()
            deleteCloudDataModalVisible = false
        }
    )

    ProgressModal(
        title = stringResource(R.string.exporting_data),
        description = stringResource(R.string.exporting_data_description),
        visible = progressState
    )
}

@Composable
private fun StartDateOfMonth(
    startDateOfMonth: Int,
    onClick: () -> Unit
) {
    SettingsButtonRow(
        onClick = onClick
    ) {
        Spacer(Modifier.width(12.dp))

        IvyIconScaled(
            icon = R.drawable.ic_custom_calendar_m,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            iconScale = IconScale.M,
            padding = 2.dp
        )

        Spacer(Modifier.width(12.dp))

        Text(
            modifier = Modifier.padding(vertical = 18.dp),
            text = stringResource(R.string.start_date_of_month),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = startDateOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.width(32.dp))
    }
}

@Composable
private fun CustomFeatures(
    onClick: () -> Unit
) {
    SettingsButtonRow(
        onClick = onClick
    ) {
        Spacer(Modifier.width(12.dp))

        IvyIconScaled(
            icon = R.drawable.ic_custom_programming_m,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            iconScale = IconScale.M,
            padding = 0.dp
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            modifier = Modifier.padding(vertical = 18.dp),
            text = stringResource(R.string.advanced_features),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AppThemeButton(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit
) {
    SettingsPrimaryButton(
        icon = icon,
        text = label,
        backgroundGradient = Gradient.solid(MaterialTheme.colorScheme.surfaceContainerHigh),
        textColor = MaterialTheme.colorScheme.onSurface,
        iconPadding = 6.dp,
        description = stringResource(R.string.tap_to_switch_theme),
        onClick = onClick
    )
}

@Composable
private fun AppSwitch(
    lockApp: Boolean,
    onSetLockApp: (Boolean) -> Unit,
    text: String,
    icon: Int,
    description: String = "",
) {
    SettingsButtonRow(
        onClick = {
            onSetLockApp(!lockApp)
        }
    ) {
        Spacer(Modifier.width(16.dp))

        IvyIconScaled(
            icon = icon,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            iconScale = IconScale.M,
            padding = 0.dp
        )

        Spacer(Modifier.width(16.dp))

        Column(
            Modifier
                .weight(1f)
                .padding(top = 16.dp, bottom = 16.dp, end = 8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (description.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    modifier = Modifier.padding(end = 8.dp),
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Switch(
            checked = lockApp,
            onCheckedChange = onSetLockApp
        )

        Spacer(Modifier.width(16.dp))
    }
}

@Composable
private fun AccountCard(
    nameLocalAccount: String?,
    onCardClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.large)
            .clickable {
                onCardClick()
            }
    ) {
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_profile_card"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(24.dp))

            Text(
                text = stringResource(R.string.account_uppercase),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(4.dp))

        AccountCardLocalAccount(
            name = nameLocalAccount,
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AccountCardLocalAccount(
    name: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(20.dp))
        IvyIconScaled(
            icon = R.drawable.ic_local_account,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            iconScale = IconScale.M
        )

        Spacer(Modifier.width(16.dp))

        Text(
            modifier = Modifier
                .weight(1f)
                .testTag("local_account_name"),
            text = if (!name.isNullOrBlank()) name else stringResource(R.string.anonymous),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.width(12.dp))
    }
}

@Composable
private fun ExportCSV(
    onExportToCSV: () -> Unit
) {
    SettingsDefaultButton(
        icon = R.drawable.ic_vue_pc_printer,
        text = stringResource(R.string.export_to_csv),
        iconPadding = 6.dp,
        description = stringResource(R.string.do_not_use_for_backup_purposes)
    ) {
        onExportToCSV()
    }
}

@Composable
private fun SettingsPrimaryButton(
    @DrawableRes icon: Int,
    text: String,
    backgroundGradient: Gradient = Gradient.solid(UI.colors.medium),
    textColor: Color = White,
    iconPadding: Dp = 0.dp,
    description: String? = null,
    onClick: () -> Unit
) {
    SettingsButtonRow(
        backgroundGradient = backgroundGradient,
        onClick = onClick
    ) {
        Spacer(Modifier.width(16.dp))

        IvyIconScaled(
            icon = icon,
            tint = textColor,
            iconScale = IconScale.M,
            padding = iconPadding
        )

        Spacer(Modifier.width(16.dp))

        Column(
            Modifier
                .weight(1f)
                .padding(top = 18.dp, bottom = 18.dp, end = 8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
            if (!description.isNullOrEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    modifier = Modifier.padding(end = 8.dp),
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsButtonRow(
    onClick: (() -> Unit)?,
    backgroundGradient: Gradient = Gradient.solid(MaterialTheme.colorScheme.surfaceContainerHigh),
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            // Flat M3 tonal surface (no legacy gradient sheen / colored shadow).
            .background(backgroundGradient.startColor, MaterialTheme.shapes.large)
            .thenIf(onClick != null) {
                clickable {
                    onClick?.invoke()
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

@Composable
private fun AccountCardButton(
    @DrawableRes icon: Int,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(UI.shapes.rFull)
            .background(UI.colors.pure, UI.shapes.rFull)
            .clickable {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(12.dp))

        IvyIconScaled(
            icon = icon,
            iconScale = IconScale.M
        )

        Spacer(Modifier.width(4.dp))

        Text(
            modifier = Modifier
                .padding(vertical = 10.dp),
            text = text,
            style = UI.typo.b2.style(
                fontWeight = FontWeight.Bold,
                color = UI.colors.pureInverse
            )
        )

        Spacer(Modifier.width(24.dp))
    }
}

@Composable
private fun CurrencyButton(
    currency: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.large)
            .clickable {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(16.dp))

        IvyIconScaled(
            icon = R.drawable.ic_currency,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            iconScale = IconScale.M,
            padding = 0.dp
        )

        Spacer(Modifier.width(16.dp))

        Text(
            modifier = Modifier.padding(vertical = 18.dp),
            text = stringResource(R.string.set_currency),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = currency,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.width(12.dp))

        IvyIconScaled(
            icon = R.drawable.ic_arrow_right,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            iconScale = IconScale.M
        )

        Spacer(Modifier.width(20.dp))
    }
}

@Composable
private fun SettingsSectionDivider(
    text: String,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Column {
        Spacer(Modifier.height(28.dp))

        Text(
            modifier = Modifier.padding(start = 32.dp),
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = color
        )
    }
}

@Composable
private fun SettingsDefaultButton(
    @DrawableRes icon: Int,
    text: String,
    iconPadding: Dp = 0.dp,
    description: String? = null,
    onClick: () -> Unit,
) {
    SettingsPrimaryButton(
        icon = icon,
        text = text,
        backgroundGradient = Gradient.solid(MaterialTheme.colorScheme.surfaceContainerHigh),
        textColor = MaterialTheme.colorScheme.onSurface,
        iconPadding = iconPadding,
        description = description
    ) {
        onClick()
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    IvyWalletPreview(theme) {
        UI(
            nameLocalAccount = null,
            theme = Theme.AUTO,
            onSwitchTheme = {},
            lockApp = false,
            currencyCode = "BGN",
            onSetCurrency = {},
            languageOptionVisible = true
        )
    }
}

/** For screenshot testing */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    Preview(theme)
}