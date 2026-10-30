package com.ivy.wallet.ui.theme.modal

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.data.model.LoanType
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.design.api.LocalTimeConverter
import com.ivy.design.l0_system.style
import com.ivy.domain.legacy.ui.IvyColorPicker
import com.ivy.frp.test.TestingContext
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.Loan
import com.ivy.legacy.legacy.ui.theme.components.DateTimeRow
import com.ivy.legacy.utils.getDefaultFIATCurrency
import com.ivy.legacy.utils.isNotNullOrBlank
import com.ivy.legacy.utils.onScreenStart
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.legacy.legacy.ui.theme.modal.ModalNameInput
import com.ivy.ui.R
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.domain.deprecated.logic.model.CreateLoanData
import com.ivy.wallet.ui.theme.Ivy
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.ivy.wallet.ui.theme.components.IvyCheckboxWithText
import com.ivy.wallet.ui.theme.modal.edit.AccountModal
import com.ivy.wallet.ui.theme.modal.edit.AccountModalData
import com.ivy.wallet.ui.theme.modal.edit.AmountModal
import com.ivy.wallet.ui.theme.modal.edit.IconNameRow
import com.ivy.wallet.ui.theme.toComposeColor
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@Deprecated("Old design system. Use `:ivy-design` and Material3")
data class LoanModalData(
    val loan: Loan?,
    val baseCurrency: String,
    val selectedAccount: Account? = null,
    val autoFocusKeyboard: Boolean = true,
    val autoOpenAmountModal: Boolean = false,
    val createLoanTransaction: Boolean = false,
    val id: UUID = UUID.randomUUID()
)

@Suppress("CyclomaticComplexMethod", "LongMethod")
@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun BoxWithConstraintsScope.LoanModal(
    modal: LoanModalData?,
    dateTime: Instant,
    onSetDate: () -> Unit,
    onSetTime: () -> Unit,
    onCreateLoan: (CreateLoanData) -> Unit,
    onEditLoan: (Loan, Boolean) -> Unit,
    accounts: List<Account> = emptyList(),
    onCreateAccount: (CreateAccountData) -> Unit = {},
    onPerformCalculations: () -> Unit = {},
    dismiss: () -> Unit,
) {
    val loan = modal?.loan
    val timeConverter = LocalTimeConverter.current

    var nameTextFieldValue by remember(modal) {
        mutableStateOf(selectEndTextFieldValue(loan?.name))
    }
    var dateTime = modal?.loan?.dateTime ?: with(timeConverter) {
        dateTime.toLocalDateTime()
    }
    var type by remember(modal) {
        mutableStateOf(modal?.loan?.type ?: LoanType.BORROW)
    }
    var amount by remember(modal) {
        mutableStateOf(modal?.loan?.amount ?: 0.0)
    }
    var color by remember(modal) {
        mutableStateOf(loan?.color?.let { Color(it) } ?: Ivy)
    }
    var icon by remember(modal) {
        mutableStateOf(loan?.icon)
    }
    var noteTextFieldValue by remember(modal) {
        mutableStateOf(selectEndTextFieldValue(loan?.note))
    }
    var currencyCode by remember(modal) {
        mutableStateOf(modal?.baseCurrency ?: "")
    }

    var selectedAcc by remember(modal) {
        mutableStateOf(modal?.selectedAccount)
    }

    var createLoanTrans by remember(modal) {
        mutableStateOf(modal?.createLoanTransaction ?: false)
    }

    var accountChangeModal by remember { mutableStateOf(false) }
    var amountModalVisible by remember { mutableStateOf(false) }
    var currencyModalVisible by remember { mutableStateOf(false) }
    var chooseIconModalVisible by remember(modal) {
        mutableStateOf(false)
    }

    var accountModalData: AccountModalData? by remember { mutableStateOf(null) }

    IvyModal(
        id = modal?.id,
        visible = modal != null,
        dismiss = dismiss,
        shiftIfKeyboardShown = false,
        PrimaryAction = {
            ModalAddSave(
                item = modal?.loan,
                // enabled = nameTextFieldValue.text.isNotNullOrBlank() && amount > 0 && ((createLoanTrans && selectedAcc != null) || !createLoanTrans)
                enabled = nameTextFieldValue.text.isNotNullOrBlank() && amount > 0 && selectedAcc != null
            ) {
                accountChangeModal =
                    loan != null && modal.selectedAccount != null && currencyCode != (
                        modal.selectedAccount.currency
                            ?: modal.baseCurrency
                        )

                if (!accountChangeModal) {
                    save(
                        loan = loan,
                        nameTextFieldValue = nameTextFieldValue,
                        dateTime = dateTime,
                        noteTextFieldValue = noteTextFieldValue,
                        type = type,
                        color = color,
                        icon = icon,
                        amount = amount,
                        selectedAccount = selectedAcc,
                        createLoanTransaction = createLoanTrans,

                        onCreateLoan = onCreateLoan,
                        onEditLoan = onEditLoan,
                        dismiss = dismiss
                    )
                }
            }
        }
    ) {
        onScreenStart {
            if (modal?.autoOpenAmountModal == true) {
                amountModalVisible = true
            }
        }

        Spacer(Modifier.height(32.dp))

        ModalTitle(
            text = if (modal?.loan != null) stringResource(R.string.edit_loan) else stringResource(R.string.new_loan),
        )

        Spacer(Modifier.height(24.dp))

        IconNameRow(
            hint = stringResource(R.string.loan_name),
            defaultIcon = R.drawable.ic_custom_loan_m,
            color = color,
            icon = icon,

            autoFocusKeyboard = modal?.autoFocusKeyboard ?: true,

            nameTextFieldValue = nameTextFieldValue,
            setNameTextFieldValue = { nameTextFieldValue = it },
            showChooseIconModal = {
                chooseIconModalVisible = true
            }
        )

        Spacer(Modifier.height(24.dp))

        DateTimeRow(
            dateTime = dateTime,
            onEditDate = onSetDate,
            onEditTime = onSetTime
        )

        Spacer(Modifier.height(24.dp))

        LoanTypePicker(
            type = type,
            onTypeSelected = { type = it }
        )

        Spacer(Modifier.height(24.dp))

        IvyColorPicker(
            selectedColor = color,
            onColorSelected = { color = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.note),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp))

        ModalNameInput(
            hint = stringResource(R.string.description_text_field_hint),
            autoFocusKeyboard = false,
            textFieldValue = noteTextFieldValue,
            setTextFieldValue = {
                noteTextFieldValue = it
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.associated_account),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(16.dp))

        AccountsRow(
            accounts = accounts,
            selectedAccount = selectedAcc,
            onSelectedAccountChanged = {
                selectedAcc = it
                currencyCode = it.currency ?: getDefaultFIATCurrency().currencyCode
            },
            onAddNewAccount = {
                accountModalData = AccountModalData(
                    account = null,
                    baseCurrency = selectedAcc?.currency ?: "USD",
                    balance = 0.0
                )
            },
            childrenTestTag = "amount_modal_account"
        )

        Spacer(Modifier.height(16.dp))

        IvyCheckboxWithText(
            modifier = Modifier
                .padding(start = 16.dp)
                .align(Alignment.Start),
            text = stringResource(R.string.create_main_transaction),
            checked = createLoanTrans
        ) {
            createLoanTrans = it
        }

        Spacer(modifier = Modifier.height(24.dp))

        ModalAmountSection(
            label = stringResource(R.string.enter_loan_amount_uppercase),
            currency = currencyCode,
            amount = amount,
            amountPaddingTop = 40.dp,
            amountPaddingBottom = 40.dp,
        ) {
            amountModalVisible = true
        }
    }

    val amountModalId = remember(modal, amount) {
        UUID.randomUUID()
    }
    AmountModal(
        id = amountModalId,
        visible = amountModalVisible,
        currency = currencyCode,
        initialAmount = amount,
        dismiss = { amountModalVisible = false }
    ) { newAmount ->
        amount = newAmount
    }

    CurrencyModal(
        title = stringResource(R.string.choose_currency),
        initialCurrency = IvyCurrency.fromCode(currencyCode),
        visible = currencyModalVisible,
        dismiss = { currencyModalVisible = false }
    ) {
        currencyCode = it
    }

    AccountModal(
        modal = accountModalData,
        onCreateAccount = onCreateAccount,
        onEditAccount = { _, _ -> },
        dismiss = {
            accountModalData = null
        }
    )

    ChooseIconModal(
        visible = chooseIconModalVisible,
        initialIcon = icon ?: "loan",
        color = color,
        dismiss = { chooseIconModalVisible = false }
    ) {
        icon = it
    }

    DeleteModal(
        visible = accountChangeModal,
        title = stringResource(R.string.confirm_account_change),
        description = stringResource(R.string.confirm_account_change_warning),
        buttonText = stringResource(R.string.confirm),
        iconStart = R.drawable.ic_agreed,
        dismiss = {
            selectedAcc = modal?.selectedAccount ?: selectedAcc
            accountChangeModal = false
        }
    ) {
        onPerformCalculations()
        save(
            loan = loan,
            nameTextFieldValue = nameTextFieldValue,
            dateTime = dateTime,
            noteTextFieldValue = noteTextFieldValue,
            type = type,
            color = color,
            icon = icon,
            amount = amount,
            selectedAccount = selectedAcc,
            createLoanTransaction = createLoanTrans,

            onCreateLoan = onCreateLoan,
            onEditLoan = onEditLoan,
            dismiss = dismiss
        )
        accountChangeModal = false
    }
}

@Composable
@Suppress("ParameterNaming")
private fun AccountsRow(
    modifier: Modifier = Modifier,
    accounts: List<Account>,
    selectedAccount: Account?,
    childrenTestTag: String? = null,
    onSelectedAccountChanged: (Account) -> Unit,
    onAddNewAccount: () -> Unit
) {
    val lazyState = rememberLazyListState()

    LaunchedEffect(accounts, selectedAccount) {
        if (selectedAccount != null) {
            val selectedIndex = accounts.indexOf(selectedAccount)
            if (selectedIndex != -1) {
                launch {
                    if (TestingContext.inTest) return@launch // breaks UI tests

                    lazyState.scrollToItem(
                        index = selectedIndex, // +1 because Spacer width 24.dp
                    )
                }
            }
        }
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        state = lazyState
    ) {
        item {
            Spacer(Modifier.width(24.dp))
        }

        itemsIndexed(accounts) { _, account ->
            Account(
                account = account,
                selected = selectedAccount == account,
                testTag = childrenTestTag ?: "account"
            ) {
                onSelectedAccountChanged(account)
            }
        }

        item {
            AddAccount {
                onAddNewAccount()
            }
        }

        item {
            Spacer(Modifier.width(24.dp))
        }
    }
}

@Composable
private fun Account(
    account: Account,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    FilterChip(
        modifier = Modifier.testTag(testTag),
        selected = selected,
        onClick = onClick,
        label = { Text(text = account.name) },
        leadingIcon = {
            ItemIconSDefaultIcon(
                modifier = Modifier.size(FilterChipDefaults.IconSize),
                iconName = account.icon,
                defaultIcon = R.drawable.ic_custom_account_s,
                tint = account.color.toComposeColor()
            )
        },
    )

    Spacer(Modifier.width(8.dp))
}

@Composable
private fun AddAccount(
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(text = stringResource(R.string.add_account)) },
        leadingIcon = {
            Icon(
                modifier = Modifier.size(AssistChipDefaults.IconSize),
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
            )
        },
    )

    Spacer(Modifier.width(8.dp))
}

@Suppress("ParameterNaming")
@Composable
private fun ColumnScope.LoanTypePicker(
    type: LoanType,
    onTypeSelected: (LoanType) -> Unit
) {
    Text(
        modifier = Modifier.padding(horizontal = 32.dp),
        text = stringResource(R.string.loan_type),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(Modifier.height(12.dp))

    val options = listOf(
        LoanType.BORROW to stringResource(R.string.borrow_money),
        LoanType.LEND to stringResource(R.string.lend_money),
    )
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
    ) {
        options.forEachIndexed { index, (option, label) ->
            SegmentedButton(
                selected = type == option,
                onClick = { onTypeSelected(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(text = label) },
            )
        }
    }
}

private fun save(
    loan: Loan?,
    nameTextFieldValue: TextFieldValue,
    dateTime: LocalDateTime,
    noteTextFieldValue: TextFieldValue,
    type: LoanType,
    color: Color,
    icon: String?,
    amount: Double,
    selectedAccount: Account? = null,
    createLoanTransaction: Boolean = false,

    onCreateLoan: (CreateLoanData) -> Unit,
    onEditLoan: (Loan, Boolean) -> Unit,
    dismiss: () -> Unit
) {
    if (loan != null) {
        onEditLoan(
            loan.copy(
                name = nameTextFieldValue.text.trim(),
                dateTime = dateTime,
                note = NotBlankTrimmedString.from(noteTextFieldValue.text).getOrNull()?.value,
                type = type,
                amount = amount,
                color = color.toArgb(),
                icon = icon,
                accountId = selectedAccount?.id
            ),
            createLoanTransaction
        )
    } else {
        onCreateLoan(
            CreateLoanData(
                name = nameTextFieldValue.text.trim(),
                type = type,
                amount = amount,
                color = color,
                icon = icon,
                account = selectedAccount,
                createLoanTransaction = createLoanTransaction,
                dateTime = dateTime,
                note = NotBlankTrimmedString.from(noteTextFieldValue.text).getOrNull()?.value
            )
        )
    }

    dismiss()
}

@Preview
@Composable
private fun Preview() {
    IvyWalletPreview {
        LoanModal(
            modal = LoanModalData(
                loan = null,
                baseCurrency = "BGN",
            ),
            onCreateLoan = { },
            onEditLoan = { _, _ -> },
            onSetTime = { },
            onSetDate = { },
            dateTime = Instant.now()
        ) {
        }
    }
}
