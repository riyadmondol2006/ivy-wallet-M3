package com.ivy.data.sync

import android.content.Context
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.ivy.base.TestDispatchersProvider
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.backup.BackupDataUseCase
import com.ivy.data.backup.ImportResult
import com.ivy.data.db.dao.fake.FakeAccountDao
import com.ivy.data.sync.impl.RedisSyncDataSourceImpl
import com.ivy.data.sync.impl.RedisTcpSyncDataSourceImpl
import com.ivy.data.sync.model.RemoteSyncMeta
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SyncRepositoryTest {
    private val backupDataUseCase = mockk<BackupDataUseCase>()
    private val rest = mockk<RedisSyncDataSourceImpl>()
    private val tcp = mockk<RedisTcpSyncDataSourceImpl>()
    private val configDataSource = mockk<SyncConfigDataSource>(relaxed = true)
    private val dataObserver = mockk<DataObserver>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)

    private lateinit var repository: SyncRepository

    private val config = SyncConfig(
        endpointUrl = "https://db.upstash.io",
        token = "secret",
        endpointType = SyncEndpointType.HTTPS,
        mode = SyncMode.AUTO,
        deviceId = "device-A",
        lastSyncedUpdatedAt = 1_000L,
    )

    @Before
    fun setup() {
        coEvery { configDataSource.get() } returns config
        coEvery { rest.putBackup(any(), any(), any(), any()) } returns Unit.right()
        coEvery { backupDataUseCase.generateJsonBackup() } returns "{}"
        repository = SyncRepository(
            backupDataUseCase = backupDataUseCase,
            restDataSource = rest,
            tcpDataSource = tcp,
            configDataSource = configDataSource,
            autoSyncGate = AutoSyncGate(),
            accountDao = FakeAccountDao(),
            dataObserver = dataObserver,
            dispatchers = TestDispatchersProvider,
            context = context,
        )
    }

    @Test
    fun `push is refused when another device wrote a newer revision`() = runTest {
        val remote = meta(deviceId = "device-B", updatedAt = 2_000L)
        coEvery { rest.getMeta(any(), any()) } returns remote.right()

        val result = repository.push()

        result.shouldBeInstanceOf<Either.Left<PushError>>()
        result.value.shouldBeInstanceOf<PushError.RemoteNewer>().remote shouldBe remote
        coVerify(exactly = 0) { rest.putBackup(any(), any(), any(), any()) }
    }

    @Test
    fun `push uploads when the remote revision is the one this device last synced`() = runTest {
        coEvery { rest.getMeta(any(), any()) } returns
            meta(deviceId = "device-B", updatedAt = 1_000L).right()

        val result = repository.push()

        result shouldBe Unit.right()
        coVerify(exactly = 1) { rest.putBackup(any(), any(), "{}", any()) }
        coVerify(exactly = 1) { configDataSource.setLastSyncedUpdatedAt(any()) }
    }

    @Test
    fun `push uploads when the remote revision came from this device`() = runTest {
        coEvery { rest.getMeta(any(), any()) } returns
            meta(deviceId = "device-A", updatedAt = 5_000L).right()

        repository.push() shouldBe Unit.right()
    }

    @Test
    fun `forced push overwrites a newer foreign revision`() = runTest {
        coEvery { rest.getMeta(any(), any()) } returns
            meta(deviceId = "device-B", updatedAt = 2_000L).right()

        repository.push(force = true) shouldBe Unit.right()
        coVerify(exactly = 1) { rest.putBackup(any(), any(), any(), any()) }
    }

    @Test
    fun `push reports a failed backup instead of throwing`() = runTest {
        coEvery { rest.getMeta(any(), any()) } returns null.right()
        coEvery { backupDataUseCase.generateJsonBackup() } throws IllegalStateException("boom")

        val result = repository.push()

        result.shouldBeInstanceOf<Either.Left<PushError>>()
            .value.shouldBeInstanceOf<PushError.Failed>().message shouldBe "boom"
        coVerify(exactly = 0) { configDataSource.setLastSyncedUpdatedAt(any()) }
    }

    @Test
    fun `pull reports a broken backup instead of throwing`() = runTest {
        coEvery { rest.getBackup(any(), any()) } returns "not json".right()
        coEvery { rest.getMeta(any(), any()) } returns null.right()
        coEvery { backupDataUseCase.importJson(any(), any(), any()) } throws
            IllegalStateException("Failed to parse backup JSON.")

        val result = repository.pull()

        result.shouldBeInstanceOf<Either.Left<String>>()
        coVerify(exactly = 0) { dataObserver.post(any()) }
    }

    @Test
    fun `pull fails cleanly when the cloud has no backup`() = runTest {
        coEvery { rest.getBackup(any(), any()) } returns null.right()

        repository.pull() shouldBe "No cloud backup found yet".left()
    }

    @Test
    fun `successful pull records the remote revision and notifies observers`() = runTest {
        val imported = ImportResult(
            rowsFound = 3,
            transactionsImported = 3,
            accountsImported = 1,
            categoriesImported = 1,
            failedRows = persistentListOf(),
        )
        coEvery { rest.getBackup(any(), any()) } returns "{}".right()
        coEvery { rest.getMeta(any(), any()) } returns
            meta(deviceId = "device-B", updatedAt = 7_000L).right()
        coEvery { backupDataUseCase.importJson(any(), any(), any()) } returns imported

        repository.pull() shouldBe imported.right()

        coVerify(exactly = 1) { configDataSource.setLastSyncedUpdatedAt(7_000L) }
        coVerify(exactly = 1) { dataObserver.post(DataWriteEvent.AllDataChange) }
    }

    @Test
    fun `test connection rejects a plain http REST url`() = runTest {
        val result = repository.testConnection(SyncEndpointType.HTTPS, "http://db.upstash.io", "t")

        result.shouldBeInstanceOf<Either.Left<String>>()
        coVerify(exactly = 0) { rest.ping(any(), any()) }
    }

    @Test
    fun `test connection rejects a non-TLS redis url`() = runTest {
        val result = repository.testConnection(SyncEndpointType.TCP, "redis://db.upstash.io:6379", "t")

        result.shouldBeInstanceOf<Either.Left<String>>()
        coVerify(exactly = 0) { tcp.ping(any(), any()) }
    }

    @Test
    fun `test connection pings a valid https url`() = runTest {
        coEvery { rest.ping("https://db.upstash.io", "t") } returns Unit.right()

        repository.testConnection(SyncEndpointType.HTTPS, "https://db.upstash.io/", " t ") shouldBe
            Unit.right()
    }

    private fun meta(deviceId: String, updatedAt: Long) = RemoteSyncMeta(
        deviceId = deviceId,
        updatedAt = updatedAt,
        accounts = 1,
        appVersion = "test",
    )
}
