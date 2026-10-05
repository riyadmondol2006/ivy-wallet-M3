package com.ivy.wallet.domain.deprecated.logic.csv

import com.ivy.legacy.domain.deprecated.logic.csv.model.ImportType
import io.kotest.matchers.shouldBe
import org.junit.Test

@OptIn(ExperimentalStdlibApi::class)
class CSVMapperTest {

    private val mapper = CSVMapper()

    @Test
    fun `ivy header with credit limit selects the V3 mapping`() {
        // A new Ivy CSV header contains both "Currency" and "Credit Limit" — V3 must win.
        val header = "Date,Title,Category,Account,Amount,Currency,Type,Transfer Amount," +
            "Transfer Currency,To Account,Receive Amount,Receive Currency,Description,Due Date," +
            "ID,Account Credit Limit,To Account Credit Limit,Account Credit Group," +
            "Account Credit Limit Shared,Account Credit Exchange Rate,To Account Credit Group," +
            "To Account Credit Limit Shared,To Account Credit Exchange Rate," +
            "Account Credit Statement Day,Account Credit Due Day," +
            "To Account Credit Statement Day,To Account Credit Due Day"

        val mapping = mapper.mapping(ImportType.IVY, header)

        mapping.accountCreditLimit shouldBe 15
        mapping.toAccountCreditLimit shouldBe 16
        mapping.accountCreditGroup shouldBe 17
        mapping.accountCreditLimitShared shouldBe 18
        mapping.accountCreditExchangeRate shouldBe 19
        mapping.toAccountCreditGroup shouldBe 20
        mapping.toAccountCreditLimitShared shouldBe 21
        mapping.toAccountCreditExchangeRate shouldBe 22
        mapping.accountCreditStatementDay shouldBe 23
        mapping.accountCreditDueDay shouldBe 24
        mapping.toAccountCreditStatementDay shouldBe 25
        mapping.toAccountCreditDueDay shouldBe 26
        // V3 does not map account color (the new export doesn't write it at index 15).
        mapping.accountColor shouldBe null
    }

    @Test
    fun `ivy header with currency but no credit limit selects the V2 mapping`() {
        val header = "Date,Title,Category,Account,Amount,Currency,Type"

        val mapping = mapper.mapping(ImportType.IVY, header)

        mapping.accountCreditLimit shouldBe null
        // V2 hallmark: currency at 5, account color at 15.
        mapping.accountCurrency shouldBe 5
        mapping.accountColor shouldBe 15
    }

    @Test
    fun `old ivy header without currency selects the V1 mapping`() {
        val header = "Date,Title,Category,Account,Amount,Type"

        val mapping = mapper.mapping(ImportType.IVY, header)

        mapping.accountCreditLimit shouldBe null
        mapping.accountCurrency shouldBe null
        // V1 hallmark: type at index 5 (no separate currency column).
        mapping.type shouldBe 5
    }
}
