package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.model.TransactionType
import com.ivy.legacy.IvyWalletPreview
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.modal.IvyModal
import com.ivy.wallet.ui.theme.modal.ModalSet
import com.ivy.wallet.ui.theme.modal.ModalTitle
import java.util.UUID

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Suppress("ParameterNaming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxWithConstraintsScope.ChangeTransactionTypeModal(
    title: String = stringResource(R.string.set_transaction_type),
    visible: Boolean,
    includeTransferType: Boolean,
    initialType: TransactionType,
    id: UUID = UUID.randomUUID(),
    dismiss: () -> Unit,
    onTransactionTypeChanged: (TransactionType) -> Unit
) {
    var transactionType by remember(id) {
        mutableStateOf(initialType)
    }

    IvyModal(
        id = id,
        visible = visible,
        dismiss = dismiss,
        PrimaryAction = {
            ModalSet {
                save(
                    transactionType = transactionType,
                    onTransactionTypeChanged = onTransactionTypeChanged,
                    dismiss = dismiss,
                )
            }
        }
    ) {
        Spacer(Modifier.height(32.dp))

        ModalTitle(text = title)

        Spacer(Modifier.height(32.dp))

        val types = buildList {
            add(TransactionType.INCOME)
            add(TransactionType.EXPENSE)
            if (includeTransferType) {
                add(TransactionType.TRANSFER)
            }
        }

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
        ) {
            types.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = transactionType == type,
                    onClick = {
                        transactionType = type
                        save(
                            transactionType = transactionType,
                            onTransactionTypeChanged = onTransactionTypeChanged,
                            dismiss = dismiss,
                        )
                    },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = types.size
                    ),
                    modifier = Modifier.testTag("modal_type_${type.name}"),
                    icon = {
                        Icon(
                            painter = painterResource(
                                id = when (type) {
                                    TransactionType.INCOME -> R.drawable.ic_income
                                    TransactionType.EXPENSE -> R.drawable.ic_expense
                                    TransactionType.TRANSFER -> R.drawable.ic_transfer
                                }
                            ),
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            text = when (type) {
                                TransactionType.INCOME -> stringResource(R.string.income)
                                TransactionType.EXPENSE -> stringResource(R.string.expense)
                                TransactionType.TRANSFER -> stringResource(R.string.transfer)
                            }
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(48.dp))
    }
}

private fun save(
    transactionType: TransactionType,
    onTransactionTypeChanged: (TransactionType) -> Unit,
    dismiss: () -> Unit
) {
    onTransactionTypeChanged(transactionType)
    dismiss()
}

@Preview
@Composable
private fun Preview() {
    IvyWalletPreview {
        ChangeTransactionTypeModal(
            includeTransferType = true,
            visible = true,
            initialType = TransactionType.INCOME,
            dismiss = {}
        ) {
        }
    }
}
