package com.ivy.data.backup

import com.ivy.base.TestDispatchersProvider
import com.ivy.base.di.KotlinxSerializationModule
import com.ivy.data.DataObserver
import com.ivy.data.db.DbTransactionRunner
import com.ivy.data.db.dao.fake.FakeAccountDao
import com.ivy.data.db.dao.fake.FakeBudgetDao
import com.ivy.data.db.dao.fake.FakeCategoryDao
import com.ivy.data.db.dao.fake.FakeLoanDao
import com.ivy.data.db.dao.fake.FakeLoanRecordDao
import com.ivy.data.db.dao.fake.FakePlannedPaymentDao
import com.ivy.data.db.dao.fake.FakeSettingsDao
import com.ivy.data.db.dao.fake.FakeTagAssociationDao
import com.ivy.data.db.dao.fake.FakeTagDao
import com.ivy.data.db.dao.fake.FakeTransactionDao
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.fake.fakeRepositoryMemoFactory
import com.ivy.data.repository.mapper.AccountMapper
import com.ivy.data.testResource
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID

class BackupDataUseCaseTest {
    private fun newBackupDataUseCase(
        accountDao: FakeAccountDao = FakeAccountDao(),
        categoryDao: FakeCategoryDao = FakeCategoryDao(),
        transactionDao: FakeTransactionDao = FakeTransactionDao(),
        plannedPaymentDao: FakePlannedPaymentDao = FakePlannedPaymentDao(),
        budgetDao: FakeBudgetDao = FakeBudgetDao(),
        settingsDao: FakeSettingsDao = FakeSettingsDao(),
        loanDao: FakeLoanDao = FakeLoanDao(),
        loanRecordDao: FakeLoanRecordDao = FakeLoanRecordDao(),
        tagDao: FakeTagDao = FakeTagDao(),
        tagAssociationDao: FakeTagAssociationDao = FakeTagAssociationDao()
    ): BackupDataUseCase {
        val accountMapper = AccountMapper(
            CurrencyRepository(
                settingsDao = settingsDao,
                writeSettingsDao = settingsDao,
                dispatchersProvider = TestDispatchersProvider,
            )
        )
        return BackupDataUseCase(
            accountDao = accountDao,
            accountMapper = accountMapper,
            accountRepository = AccountRepository(
                accountDao = accountDao,
                writeAccountDao = accountDao,
                mapper = accountMapper,
                dispatchersProvider = TestDispatchersProvider,
                memoFactory = fakeRepositoryMemoFactory(),
            ),
            budgetDao = budgetDao,
            categoryDao = categoryDao,
            loanRecordDao = loanRecordDao,
            loanDao = loanDao,
            plannedPaymentRuleDao = plannedPaymentDao,
            transactionDao = transactionDao,
            transactionWriter = transactionDao,
            settingsDao = settingsDao,
            categoryWriter = categoryDao,
            settingsWriter = settingsDao,
            budgetWriter = budgetDao,
            loanWriter = loanDao,
            loanRecordWriter = loanRecordDao,
            plannedPaymentRuleWriter = plannedPaymentDao,

            context = mockk(relaxed = true),
            sharedPrefs = mockk(relaxed = true),
            json = KotlinxSerializationModule.provideJson(),
            dispatchersProvider = TestDispatchersProvider,
            fileSystem = mockk(relaxed = true),
            dataObserver = DataObserver(),
            tagsReader = tagDao,
            tagsWriter = tagDao,
            tagAssociationReader = tagAssociationDao,
            tagAssociationWriter = tagAssociationDao,
            categoryRepository = mockk(relaxed = true),
            tagRepository = mockk(relaxed = true),
            transactionRunner = passThroughTransactions,
        )
    }

    // The fakes have no real transactions; atomicity is covered on a device. Here we only check
    // that the backup is parsed before anything is deleted.
    private val passThroughTransactions = object : DbTransactionRunner {
        override suspend fun <T> inTransaction(block: suspend () -> T): T = block()
    }

    private suspend fun backupTestCase(backupVersion: String) {
        // given
        val originalBackupUseCase = newBackupDataUseCase()
        val backupJsonData = testResource("backups/$backupVersion.json")
            .readText(Charsets.UTF_16)

        // when
        val importedDataRes = originalBackupUseCase.importJson(backupJsonData, onProgress = {})

        // then
        importedDataRes.accountsImported shouldBeGreaterThan 0
        importedDataRes.transactionsImported shouldBeGreaterThan 0
        importedDataRes.categoriesImported shouldBeGreaterThan 0
        importedDataRes.failedRows.size shouldBe 0

        // Also - exporting and re-importing the data should work
        // given
        val exportedJson = originalBackupUseCase.generateJsonBackup()

        // when
        val freshBackupUseCase = newBackupDataUseCase()
        val reImportedDataRes = freshBackupUseCase.importJson(exportedJson, onProgress = {})
        // then
        reImportedDataRes shouldBe importedDataRes

        // Finally, exporting again should yield the same result
        freshBackupUseCase.generateJsonBackup() shouldBe exportedJson
    }

    @Test
    fun `backup compatibility with 450 (150)`() = runTest {
        backupTestCase("450-150")
    }

    @Test
    fun `credit card creditLimit round-trips through a backup`() = runTest {
        // given - a DB containing a credit-card account (Account with a non-null creditLimit)
        val sourceAccountDao = FakeAccountDao()
        sourceAccountDao.save(
            AccountEntity(
                name = "Visa",
                currency = "USD",
                color = 1,
                includeInBalance = false,
                creditLimit = 5000.0,
                id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
            )
        )
        val source = newBackupDataUseCase(accountDao = sourceAccountDao)

        // when - export, then import into a fresh DB
        val exportedJson = source.generateJsonBackup()
        val freshAccountDao = FakeAccountDao()
        newBackupDataUseCase(accountDao = freshAccountDao)
            .importJson(exportedJson, onProgress = {})

        // then - the imported account is still a credit card
        val imported = freshAccountDao.findAll().first { it.name == "Visa" }
        imported.creditLimit shouldBe 5000.0
    }

    @Test
    fun `credit card billing cycle round trips through backup`() = runTest {
        val sourceDao = FakeAccountDao()
        sourceDao.save(
            AccountEntity(
                name = "Visa",
                currency = "USD",
                color = 1,
                creditLimit = 1000.0,
                creditStatementDay = 5,
                creditDueDay = 25,
                id = UUID.randomUUID()
            )
        )
        val json = newBackupDataUseCase(accountDao = sourceDao).generateJsonBackup()
        val targetDao = FakeAccountDao()
        newBackupDataUseCase(accountDao = targetDao).importJson(json, onProgress = {})
        val restored = targetDao.findAll().single()
        restored.creditStatementDay shouldBe 5
        restored.creditDueDay shouldBe 25
    }

    @Test
    fun `dual currency card metadata round trips through backup`() = runTest {
        val id = UUID.randomUUID()
        val sourceDao = FakeAccountDao()
        val primary = AccountEntity(
            name = "Visa", currency = "BDT", color = 1,
            creditLimit = 100000.0, creditCardGroupId = id, creditLimitShared = true,
            creditExchangeRate = 120.0, includeInBalance = false, id = id
        )
        val secondary = primary.copy(
            id = UUID.randomUUID(),
            name = "Visa · USD",
            currency = "USD",
            creditLimit = 1000.0,
            creditLimitShared = false,
            creditExchangeRate = null
        )
        sourceDao.save(primary)
        sourceDao.save(secondary)
        val json = newBackupDataUseCase(accountDao = sourceDao).generateJsonBackup()
        val targetDao = FakeAccountDao()
        newBackupDataUseCase(accountDao = targetDao).importJson(json, onProgress = {})
        val result = targetDao.findAll().associateBy { it.id }
        result[id]?.creditCardGroupId shouldBe id
        result[id]?.creditLimitShared shouldBe true
        result[id]?.creditExchangeRate shouldBe 120.0
        result[secondary.id]?.creditCardGroupId shouldBe id
        result[secondary.id]?.creditLimit shouldBe 1000.0
        result[secondary.id]?.currency shouldBe "USD"
    }

    @Test
    fun `old backup without creditLimit imports as a normal account`() = runTest {
        // given - the 450-150 backup predates credit cards (no creditLimit field in its JSON)
        val accountDao = FakeAccountDao()
        val useCase = newBackupDataUseCase(accountDao = accountDao)
        val oldJson = testResource("backups/450-150.json").readText(Charsets.UTF_16)

        // when
        useCase.importJson(oldJson, onProgress = {})

        // then - every imported account is a normal account (null creditLimit), no crash
        val accounts = accountDao.findAll()
        accounts.size shouldBeGreaterThan 0
        accounts.forEach {
            it.creditLimit shouldBe null
            it.creditCardGroupId shouldBe null
            it.creditLimitShared shouldBe false
            it.creditExchangeRate shouldBe null
        }
    }

    @Test
    fun `replacing makes the local data an exact copy of the backup`() = runTest {
        // given - a backup, and a device that has the same data plus items deleted elsewhere
        val sourceAccounts = FakeAccountDao()
        val sourceTransactions = FakeTransactionDao()
        val source = newBackupDataUseCase(
            accountDao = sourceAccounts,
            transactionDao = sourceTransactions,
        )
        source.importJson(testResource("backups/450-150.json").readText(Charsets.UTF_16))
        val backupJson = source.generateJsonBackup()

        val targetAccounts = FakeAccountDao()
        val targetTransactions = FakeTransactionDao()
        val target = newBackupDataUseCase(
            accountDao = targetAccounts,
            transactionDao = targetTransactions,
        )
        target.importJson(backupJson)
        val deletedElsewhere = AccountEntity(
            name = "Deleted on the other phone",
            currency = "USD",
            color = 1,
            id = UUID.randomUUID(),
        )
        targetAccounts.save(deletedElsewhere)

        // when
        target.replaceAllWithJson(backupJson)

        // then
        targetAccounts.findAll().map { it.id } shouldContainExactlyInAnyOrder
            sourceAccounts.findAll().map { it.id }
        targetTransactions.findAll().map { it.id } shouldContainExactlyInAnyOrder
            sourceTransactions.findAll().map { it.id }
        target.generateJsonBackup() shouldBe backupJson
    }

    @Test
    fun `a broken backup never deletes local data`() = runTest {
        val accounts = FakeAccountDao()
        val useCase = newBackupDataUseCase(accountDao = accounts)
        useCase.importJson(testResource("backups/450-150.json").readText(Charsets.UTF_16))
        val before = accounts.findAll()

        shouldThrowAny { useCase.replaceAllWithJson("{\"accounts\": [{\"broken\": ") }

        accounts.findAll() shouldBe before
    }

    @Test
    fun `cloud backups written by v1_0_1 and v1_0_8 restore with both strategies`() = runTest {
        listOf("m3-v1.0.1-cloud", "m3-v1.0.8-cloud").forEach { name ->
            val legacyJson = testResource("backups/$name.json").readText(Charsets.UTF_8)

            val replaced = newBackupDataUseCase().replaceAllWithJson(legacyJson)
            val merged = newBackupDataUseCase().importJson(legacyJson)

            replaced.accountsImported shouldBe 2
            replaced.categoriesImported shouldBe 2
            replaced.transactionsImported shouldBe 1
            merged shouldBe replaced
        }
    }

    @Test
    fun `file backups written by v1_0_1 still import`() = runTest {
        val accounts = FakeAccountDao()
        val legacyJson = testResource("backups/m3-v1.0.1-file.json").readText(Charsets.UTF_16)

        val result = newBackupDataUseCase(accountDao = accounts).importJson(legacyJson)

        result.transactionsImported shouldBe 1
        accounts.findAll().map { it.name } shouldContainExactlyInAnyOrder listOf("Cash", "Bank")
    }
}
