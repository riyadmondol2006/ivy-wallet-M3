package com.ivy.legacy.domain

import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeProvider
import com.ivy.data.db.dao.write.WritePlannedPaymentRuleDao
import com.ivy.data.db.entity.TransactionEntity
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.data.repository.mapper.TransactionMapper
import com.ivy.legacy.data.model.CreditCardInput
import com.ivy.legacy.data.model.CreditCardPaymentInput
import com.ivy.wallet.domain.action.account.CalcAccBalanceAct
import com.ivy.wallet.domain.data.IvyCurrency
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreditCardService @Inject constructor(
    private val accounts: AccountRepository,
    private val transactions: TransactionRepository,
    private val plannedPaymentRuleWriter: WritePlannedPaymentRuleDao,
    private val transactionMapper: TransactionMapper,
    private val balance: CalcAccBalanceAct,
    private val timeProvider: TimeProvider,
) {
    private val mutex = Mutex()

    @Suppress("CyclomaticComplexMethod")
    suspend fun save(input: CreditCardInput): Unit = mutex.withLock {
        require(input.name.isNotBlank())
        require(validMoney(input.limit, input.currency))
        require((input.statementDay == null) == (input.dueDay == null)) {
            "Statement day and due day must be set together"
        }
        require(input.statementDay?.let { it in 1..DAYS_IN_LONGEST_MONTH } ?: true)
        require(input.dueDay?.let { it in 1..DAYS_IN_LONGEST_MONTH } ?: true)
        val existing = input.primaryId?.let { accounts.findById(it) }
        require(input.primaryId == null || existing?.creditLimit != null)
        if (existing != null) require(existing.asset.code == input.currency)
        val primaryId = existing?.id ?: AccountId(UUID.randomUUID())
        val previousSecondary = accounts.findAll().firstOrNull {
            it.id != primaryId && it.creditCardGroupId == primaryId
        }
        val dual = input.secondaryCurrency != null
        if (previousSecondary != null) {
            // Currency history is immutable; changing/removing a ledger would reinterpret debt.
            require(input.secondaryCurrency == previousSecondary.asset.code)
        }
        if (dual) {
            require(input.secondaryCurrency != input.currency)
            require(input.secondaryLimit?.let { validMoney(it, input.secondaryCurrency!!) } == true)
            if (input.sharedLimit) {
                require(input.exchangeRate?.let { it.isFinite() && it > 0.0 } == true)
            }
        }
        val primary = Account(
            id = primaryId,
            name = NotBlankTrimmedString.unsafe(input.name.trim()),
            asset = AssetCode.unsafe(input.currency),
            color = ColorInt(input.color),
            icon = input.icon?.let { IconAsset.from(it).getOrNull() },
            includeInBalance = input.includeInBalance,
            orderNum = existing?.orderNum ?: (accounts.findMaxOrderNum() + 1.0),
            creditLimit = input.limit,
            creditCardGroupId = if (dual) primaryId else null,
            creditLimitShared = dual && input.sharedLimit,
            creditExchangeRate = input.exchangeRate.takeIf { dual && input.sharedLimit },
            creditStatementDay = input.statementDay,
            creditDueDay = input.dueDay,
        )
        val secondary = if (dual) {
            primary.copy(
                id = previousSecondary?.id ?: AccountId(UUID.randomUUID()),
                name = NotBlankTrimmedString.unsafe("${input.name.trim()} · ${input.secondaryCurrency}"),
                asset = AssetCode.unsafe(input.secondaryCurrency!!),
                orderNum = previousSecondary?.orderNum ?: (primary.orderNum + SECONDARY_ORDER_OFFSET),
                creditLimit = input.secondaryLimit,
                creditLimitShared = false,
                creditExchangeRate = null,
                // The billing cycle belongs to the physical card, i.e. the primary ledger.
                creditStatementDay = null,
                creditDueDay = null,
            )
        } else {
            null
        }
        // Room's list upsert commits both currency accounts together; existing transactions stay put.
        accounts.saveMany(listOfNotNull(primary, secondary))
    }

    suspend fun pay(input: CreditCardPaymentInput, title: String): Unit = mutex.withLock {
        val card = requireNotNull(accounts.findById(input.cardAccountId))
        val source = requireNotNull(accounts.findById(input.fromAccountId))
        require(card.creditLimit != null && source.creditLimit == null && source.id != card.id)
        require(validMoney(input.paidAmount, card.asset.code))
        require(validMoney(input.debitedAmount, source.asset.code))
        if (source.asset == card.asset) {
            require(input.paidAmount.toBigDecimal() == input.debitedAmount.toBigDecimal())
        }
        val owed = (-balance(CalcAccBalanceAct.Input(card)).balance).max(BigDecimal.ZERO)
        require(input.paidAmount.toBigDecimal() <= owed) { "Payment exceeds the current amount to pay" }
        val entity = TransactionEntity(
            accountId = source.id.value,
            toAccountId = card.id.value,
            type = TransactionType.TRANSFER,
            amount = input.debitedAmount,
            toAmount = input.paidAmount,
            title = title,
            dateTime = timeProvider.utcNow(),
        )
        val transfer = with(transactionMapper) { entity.toDomain() }.getOrNull()
        requireNotNull(transfer)
        transactions.save(transfer)
    }

    /**
     * Deletes a credit card together with every currency ledger in its group, their transactions and
     * planned payments. Returns the ids that were removed.
     */
    suspend fun delete(cardId: AccountId): List<AccountId> = mutex.withLock {
        val selected = accounts.findById(cardId) ?: return@withLock emptyList()
        val group = selected.creditCardGroupId
        val ids = if (group == null) {
            listOf(selected.id)
        } else {
            accounts.findAll().filter { it.creditCardGroupId == group }.map { it.id }
        }
        ids.forEach { id ->
            transactions.deleteAllByAccountId(id)
            plannedPaymentRuleWriter.deletedByAccountId(id.value)
            accounts.deleteById(id)
        }
        ids
    }

    suspend fun reset(cardId: AccountId, expectedOwed: Double, title: String): Unit = mutex.withLock {
        val card = requireNotNull(accounts.findById(cardId))
        require(card.creditLimit != null)
        val owed = (-balance(CalcAccBalanceAct.Input(card)).balance).max(BigDecimal.ZERO)
        // Do not reset a changed amount that the user did not see in the confirmation.
        require(owed.signum() > 0 && owed.compareTo(expectedOwed.toBigDecimal()) == 0)
        val entity = TransactionEntity(
            accountId = card.id.value,
            type = TransactionType.INCOME,
            amount = owed.toDouble(),
            title = title,
            dateTime = timeProvider.utcNow()
        )
        val adjustment = with(transactionMapper) { entity.toDomain() }.getOrNull()
        transactions.save(requireNotNull(adjustment))
    }

    private companion object {
        /** Keeps the secondary currency ledger right after its primary account in sort order. */
        const val SECONDARY_ORDER_OFFSET = 0.001
        const val DAYS_IN_LONGEST_MONTH = 31
    }
}

fun validMoney(amount: Double, currency: String): Boolean =
    amount.isFinite() && amount > 0.0 &&
        amount.toBigDecimal().stripTrailingZeros().scale() <= IvyCurrency.getDecimalPlaces(currency)

/** Accept the local decimal separator, but never reinterpret grouping commas as decimal points. */
fun parseCreditAmount(text: String, locale: java.util.Locale = java.util.Locale.getDefault()): Double? {
    val separator = java.text.DecimalFormatSymbols.getInstance(locale).decimalSeparator
    val normalized = text.trim().map { character ->
        when {
            character == separator -> '.'
            character.digitToIntOrNull() != null -> ('0'.code + character.digitToInt()).toChar()
            else -> character
        }
    }.joinToString("")
    if (!normalized.matches(Regex("[0-9]*(\\.[0-9]*)?"))) return null
    return normalized.toDoubleOrNull()?.takeIf { it.isFinite() }
}
