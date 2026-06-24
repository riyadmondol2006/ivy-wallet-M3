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
            "ID,Account Credit Limit,To Account Credit Limit"

        val mapping = mapper.mapping(ImportType.IVY, header)

        mapping.accountCreditLimit shouldBe 15
        mapping.toAccountCreditLimit shouldBe 16
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
