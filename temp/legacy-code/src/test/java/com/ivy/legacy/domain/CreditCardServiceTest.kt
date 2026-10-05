package com.ivy.legacy.domain

import com.ivy.base.time.TimeProvider
import com.ivy.data.db.dao.write.WritePlannedPaymentRuleDao
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.Income
import com.ivy.data.model.Transaction
import com.ivy.data.model.Transfer
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.data.repository.mapper.TransactionMapper
import com.ivy.legacy.data.model.CreditCardInput
import com.ivy.legacy.data.model.CreditCardPaymentInput
import com.ivy.wallet.domain.action.account.CalcAccBalanceAct
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class CreditCardServiceTest {
    private val accounts = mockk<AccountRepository>(relaxed = true)
    private val transactions = mockk<TransactionRepository>(relaxed = true)
    private val mapper = TransactionMapper(accounts)
    private val balance = mockk<CalcAccBalanceAct>()
    private val time = mockk<TimeProvider> {
        every { utcNow() } returns java.time.Instant.parse("2026-09-06T00:00:00Z")
    }
    private val plannedPayments = mockk<WritePlannedPaymentRuleDao>(relaxed = true)
    private val service = CreditCardService(accounts, transactions, plannedPayments, mapper, balance, time)
    private val primary = Account(
        AccountId(UUID.randomUUID()),
        NotBlankTrimmedString.unsafe("Visa"),
        AssetCode.unsafe("BDT"),
        ColorInt(1),
        null,
        false,
        2.0,
        creditLimit = 100000.0,
    )
    private fun input() = CreditCardInput(
        primaryId = primary.id,
        name = "Updated Visa",
        currency = "BDT",
        limit = 150000.0,
        color = 2,
        icon = null,
        secondaryCurrency = "USD",
        secondaryLimit = 1000.0,
        sharedLimit = true,
        exchangeRate = 120.0,
    )

    @Test fun convertingExistingCardPreservesIdAndCreatesLinkedEmptyLedger() = runTest {
        coEvery { accounts.findById(primary.id) } returns primary
        coEvery { accounts.findAll() } returns listOf(primary)
        val saved = slot<List<Account>>()
        coEvery { accounts.saveMany(capture(saved)) } just Runs
        service.save(input())
        assertEquals(2, saved.captured.size)
        val main = saved.captured[0]
        val second = saved.captured[1]
        assertEquals(primary.id, main.id)
        assertEquals(primary.orderNum, main.orderNum, 0.0)
        assertEquals(main.id, main.creditCardGroupId)
        assertEquals(main.id, second.creditCardGroupId)
        assertEquals("USD", second.asset.code)
        assertNotEquals(main.id, second.id)
        coVerify(exactly = 0) { transactions.save(any()) }
    }

    @Test fun editingDualCardPreservesBothIdsAndIndependentModeKeepsOptionalRate() = runTest {
        val main = primary.copy(creditCardGroupId = primary.id)
        val second = primary.copy(
            id = AccountId(UUID.randomUUID()),
            asset = AssetCode.unsafe("USD"),
            creditCardGroupId = primary.id
        )
        coEvery { accounts.findById(primary.id) } returns main
        coEvery { accounts.findAll() } returns listOf(main, second)
        val saved = slot<List<Account>>()
        coEvery { accounts.saveMany(capture(saved)) } just Runs
        service.save(input().copy(sharedLimit = false))
        assertEquals(second.id, saved.captured[1].id)
        assertFalse(saved.captured[0].creditLimitShared)
        assertEquals(120.0, saved.captured[0].creditExchangeRate!!, 0.0)
        assertNull(saved.captured[1].creditExchangeRate)

        service.save(input().copy(sharedLimit = false, exchangeRate = null))
        assertNull(saved.captured[0].creditExchangeRate)
    }

    @Test fun sharedLimitStillRequiresABankRate() = runTest {
        coEvery { accounts.findById(primary.id) } returns primary
        coEvery { accounts.findAll() } returns listOf(primary)
        for (rate in listOf(null, 0.0, -1.0, Double.NaN)) {
            try {
                service.save(input().copy(sharedLimit = true, exchangeRate = rate))
                fail("A shared limit cannot estimate available credit without a rate")
            } catch (_: IllegalArgumentException) { }
        }
        coVerify(exactly = 0) { accounts.saveMany(any()) }
    }

    @Test fun existingSecondaryCurrencyCannotBeRemovedOrReinterpreted() = runTest {
        val second = primary.copy(
            id = AccountId(UUID.randomUUID()),
            asset = AssetCode.unsafe("USD"),
            creditCardGroupId = primary.id
        )
        coEvery { accounts.findById(primary.id) } returns primary
        coEvery { accounts.findAll() } returns listOf(primary, second)
        for (currency in listOf(null, "EUR")) {
            try {
                service.save(input().copy(secondaryCurrency = currency))
                fail("Should reject a destructive currency change")
            } catch (_: IllegalArgumentException) { }
        }
        coVerify(exactly = 0) { accounts.saveMany(any()) }
    }

    @Test fun overpaymentIsRejectedAgainstFreshBalance() = runTest {
        val bank = primary.copy(id = AccountId(UUID.randomUUID()), creditLimit = null)
        coEvery { accounts.findById(primary.id) } returns primary
        coEvery { accounts.findById(bank.id) } returns bank
        coEvery { balance(any()) } returns CalcAccBalanceAct.Output(primary, (-50).toBigDecimal())
        try {
            service.pay(CreditCardPaymentInput(primary.id, bank.id, 100.0, 100.0), "Payment")
            fail("Should reject stale overpayment")
        } catch (_: IllegalArgumentException) { }
        coVerify(exactly = 0) { transactions.save(any()) }
    }

    @Test fun crossCurrencyPartialPaymentRecordsActualDebitAndCredit() = runTest {
        val card = primary.copy(asset = AssetCode.unsafe("USD"))
        val bank = primary.copy(id = AccountId(UUID.randomUUID()), creditLimit = null)
        coEvery { accounts.findById(card.id) } returns card
        coEvery { accounts.findById(bank.id) } returns bank
        coEvery { balance(any()) } returns CalcAccBalanceAct.Output(card, (-100).toBigDecimal())
        val saved = slot<Transaction>()
        coEvery { transactions.save(capture(saved)) } just Runs
        service.pay(CreditCardPaymentInput(card.id, bank.id, 50.0, 6250.0), "Payment")
        val transfer = saved.captured as Transfer
        assertEquals(bank.id, transfer.fromAccount)
        assertEquals(card.id, transfer.toAccount)
        assertEquals("BDT", transfer.fromValue.asset.code)
        assertEquals("USD", transfer.toValue.asset.code)
        assertEquals(6250.0, transfer.fromValue.amount.value, 0.0)
        assertEquals(50.0, transfer.toValue.amount.value, 0.0)
        assertTrue(transfer.settled)
    }

    @Test fun sameCurrencyPaymentUsesEqualAmounts() = runTest {
        val bank = primary.copy(id = AccountId(UUID.randomUUID()), creditLimit = null)
        coEvery { accounts.findById(primary.id) } returns primary
        coEvery { accounts.findById(bank.id) } returns bank
        coEvery { balance(any()) } returns CalcAccBalanceAct.Output(primary, (-100).toBigDecimal())
        val saved = slot<Transaction>()
        coEvery { transactions.save(capture(saved)) } just Runs
        service.pay(CreditCardPaymentInput(primary.id, bank.id, 100.0, 100.0), "Payment")
        val transfer = saved.captured as Transfer
        assertEquals(transfer.fromValue, transfer.toValue)
        try {
            service.pay(CreditCardPaymentInput(primary.id, bank.id, 50.0, 51.0), "Payment")
            fail("Same-currency amounts must match")
        } catch (_: IllegalArgumentException) { }
        coVerify(exactly = 1) { transactions.save(any()) }
    }

    @Test fun resetIsAnAdjustmentOnlyAndRejectsChangedBalance() = runTest {
        coEvery { accounts.findById(primary.id) } returns primary
        coEvery { balance(any()) } returns CalcAccBalanceAct.Output(primary, (-100).toBigDecimal())
        val saved = slot<Transaction>()
        coEvery { transactions.save(capture(saved)) } just Runs
        service.reset(primary.id, 100.0, "Adjustment")
        val adjustment = saved.captured as Income
        assertEquals(primary.id, adjustment.account)
        assertEquals(100.0, adjustment.value.amount.value, 0.0)
        try {
            service.reset(primary.id, 50.0, "Adjustment")
            fail("Must not reset an unconfirmed balance")
        } catch (_: IllegalArgumentException) { }
        coVerify(exactly = 1) { transactions.save(any()) }
    }
}
