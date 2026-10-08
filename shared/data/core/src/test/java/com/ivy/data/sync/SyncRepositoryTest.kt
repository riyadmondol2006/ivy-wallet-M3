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
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.sync.impl.RedisSyncDataSourceImpl
import com.ivy.data.sync.impl.RedisTcpSyncDataSourceImpl
import com.ivy.data.sync.model.RemoteSyncMeta
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.UUID

class SyncRepositoryTest {
    private val backupDataUseCase = mockk<BackupDataUseCase>()
    private val rest = mockk<RedisSyncDataSourceImpl>()
    private val tcp = mockk<RedisTcpSyncDataSourceImpl>()
    private val configDataSource = mockk<SyncConfigDataSource>(relaxed = true)
    private val dataObserver = mockk<DataObserver>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val accountDao = FakeAccountDao()

    private lateinit var repository: SyncRepository

    private val config = SyncConfig(
        endpointUrl = "https://db.upstash.io",
        token = "secret",
        endpointType = SyncEndpointType.HTTPS,
        mode = SyncMode.AUTO,
        deviceId = "device-A",
        lastSyncedUpdatedAt = 1_000L,
        localChangedAt = 0L,
    )

    private val imported = ImportResult(
        rowsFound = 3,
        transactionsImported = 3,
        accountsImported = 1,
        categoriesImported = 1,
        failedRows = persistentListOf(),
    )

    /** What the config store currently holds; writes to it are reflected in later reads. */
    private var storedConfig = config

    private fun givenConfig(value: SyncConfig) {
        storedConfig = value
    }

    @Before
    fun setup() {
        coEvery { configDataSource.get() } answers { storedConfig }
        coEvery { configDataSource.setLastSyncedUpdatedAt(any()) } answers {
            storedConfig = storedConfig.copy(lastSyncedUpdatedAt = firstArg())
        }
        coEvery { rest.putBackup(any(), any(), any(), any()) } returns Unit.right()
        coEvery { backupDataUseCase.generateJsonBackup() } returns "{}"
        coEvery { backupDataUseCase.replaceAllWithJson(any()) } returns imported
        coEvery { backupDataUseCase.importJson(any(), any(), any()) } returns imported
        repository = SyncRepository(
            backupDataUseCase = backupDataUseCase,
            restDataSource = rest,
            tcpDataSource = tcp,
            configDataSource = configDataSource,
            autoSyncGate = AutoSyncGate(),
            accountDao = accountDao,
            dataObserver = dataObserver,
            dispatchers = TestDispatchersProvider,
            context = context,
        )
    }

    // region push
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
        coVerify(exactly = 1) { configDataSource.clearLocalChange(any()) }
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
    fun `a backup without meta from a never-synced database is not overwritten`() = runTest {
        // An interrupted upload from v1.0.1-1.0.3 can leave the backup without its meta record.
        givenConfig(config.copy(lastSyncedUpdatedAt = 0L))
        coEvery { rest.getMeta(any(), any()) } returns RemoteSyncMeta.unknown().right()

        repository.push().shouldBeInstanceOf<Either.Left<PushError>>()
            .value.shouldBeInstanceOf<PushError.RemoteNewer>()
        coVerify(exactly = 0) { rest.putBackup(any(), any(), any(), any()) }
    }

    @Test
    fun `push reports a failed backup instead of throwing`() = runTest {
        coEvery { rest.getMeta(any(), any()) } returns null.right()
        coEvery { backupDataUseCase.generateJsonBackup() } throws IllegalStateException("boom")

        val result = repository.push()

        result.shouldBeInstanceOf<Either.Left<PushError>>()
            .value.shouldBeInstanceOf<PushError.Failed>().message shouldBe "boom"
        coVerify(exactly = 0) { configDataSource.setLastSyncedUpdatedAt(any()) }
        coVerify(exactly = 0) { configDataSource.clearLocalChange(any()) }
    }

    @Test
    fun `push compresses a backup too large for one Upstash request`() = runTest {
        coEvery { rest.getMeta(any(), any()) } returns null.right()
        val huge = "{\"transactions\":\"" + "x".repeat(CloudBackupCodec.COMPRESS_THRESHOLD_BYTES) + "\"}"
        coEvery { backupDataUseCase.generateJsonBackup() } returns huge
        val stored = slot<String>()
        coEvery { rest.putBackup(any(), any(), capture(stored), any()) } returns Unit.right()

        repository.push() shouldBe Unit.right()

        stored.captured shouldStartWith CloudBackupCodec.COMPRESSED_PREFIX
        CloudBackupCodec.decode(stored.captured) shouldBe huge
    }
    // endregion

    // region pull
    @Test
    fun `pull replaces local data when this device has no unsynced changes`() = runTest {
        coEvery { rest.getSnapshot(any(), any()) } returns
            RemoteSnapshot("{}", meta(deviceId = "device-B", updatedAt = 7_000L)).right()

        repository.pull() shouldBe imported.right()

        coVerify(exactly = 1) { backupDataUseCase.replaceAllWithJson("{}") }
        coVerify(exactly = 0) { backupDataUseCase.importJson(any(), any(), any()) }
        coVerify(exactly = 1) { configDataSource.setLastSyncedUpdatedAt(7_000L) }
        coVerify(exactly = 1) { configDataSource.clearLocalChange(any()) }
        coVerify(exactly = 0) { rest.putBackup(any(), any(), any(), any()) }
        coVerify(exactly = 1) { dataObserver.post(DataWriteEvent.AllDataChange) }
    }

    @Test
    fun `pull merges and pushes back when this device has unsynced changes`() = runTest {
        givenConfig(config.copy(localChangedAt = 500L))
        val remote = meta(deviceId = "device-B", updatedAt = 7_000L)
        coEvery { rest.getSnapshot(any(), any()) } returns RemoteSnapshot("{}", remote).right()
        // After the merge the cloud still holds device B's revision.
        coEvery { rest.getMeta(any(), any()) } returns remote.right()

        repository.pull() shouldBe imported.right()

        coVerify(exactly = 1) { backupDataUseCase.importJson("{}", any(), any()) }
        coVerify(exactly = 0) { backupDataUseCase.replaceAllWithJson(any()) }
        coVerify(exactly = 1) { configDataSource.setLastSyncedUpdatedAt(7_000L) }
        coVerify(exactly = 1) { rest.putBackup(any(), any(), any(), any()) }
    }

    @Test
    fun `pull merges when the device had data before it first synced`() = runTest {
        givenConfig(config.copy(lastSyncedUpdatedAt = 0L))
        accountDao.save(AccountEntity(name = "Mine", currency = "USD", color = 1, id = UUID.randomUUID()))
        coEvery { rest.getSnapshot(any(), any()) } returns
            RemoteSnapshot("{}", meta(deviceId = "device-B", updatedAt = 7_000L)).right()
        coEvery { rest.getMeta(any(), any()) } returns
            meta(deviceId = "device-B", updatedAt = 7_000L).right()

        repository.pull()

        coVerify(exactly = 1) { backupDataUseCase.importJson(any(), any(), any()) }
        coVerify(exactly = 0) { backupDataUseCase.replaceAllWithJson(any()) }
    }

    @Test
    fun `pull reads a compressed backup`() = runTest {
        val json = "{\"accounts\":[]}" + " ".repeat(CloudBackupCodec.COMPRESS_THRESHOLD_BYTES)
        val stored = CloudBackupCodec.encode(json)
        stored shouldStartWith CloudBackupCodec.COMPRESSED_PREFIX
        coEvery { rest.getSnapshot(any(), any()) } returns RemoteSnapshot(stored, null).right()

        repository.pull() shouldBe imported.right()

        coVerify(exactly = 1) { backupDataUseCase.replaceAllWithJson(json) }
    }

    @Test
    fun `pull reports a broken backup instead of throwing`() = runTest {
        coEvery { rest.getSnapshot(any(), any()) } returns RemoteSnapshot("not json", null).right()
        coEvery { backupDataUseCase.replaceAllWithJson(any()) } throws
            IllegalStateException("Failed to parse backup JSON.")

        val result = repository.pull()

        result.shouldBeInstanceOf<Either.Left<String>>()
        coVerify(exactly = 0) { dataObserver.post(any()) }
        coVerify(exactly = 0) { configDataSource.setLastSyncedUpdatedAt(any()) }
    }

    @Test
    fun `pull fails cleanly when the cloud has no backup`() = runTest {
        coEvery { rest.getSnapshot(any(), any()) } returns RemoteSnapshot(null, null).right()

        repository.pull() shouldBe "No cloud backup found yet".left()
    }
    // endregion

    @Test
    fun `remote status reports a conflict when both devices changed data`() = runTest {
        givenConfig(config.copy(localChangedAt = 500L))
        coEvery { rest.getMeta(any(), any()) } returns
            meta(deviceId = "device-B", updatedAt = 7_000L).right()

        val status = repository.checkRemote()

        status.shouldPromptPull shouldBe true
        status.isConflict shouldBe true
    }

    // region connection
    @Test
    fun `test connection rejects a plain http REST url`() = runTest {
        val result = repository.testConnection(SyncEndpointType.HTTPS, "http://db.upstash.io", "t")

        result.shouldBeInstanceOf<Either.Left<String>>()
        coVerify(exactly = 0) { rest.ping(any(), any()) }
    }

    @Test
    fun `test connection accepts the redis-cli command the Upstash console shows`() = runTest {
        val command = "redis-cli --tls -u redis://default:pw@db.upstash.io:6379"
        coEvery { tcp.ping(command, "") } returns Unit.right()

        repository.testConnection(SyncEndpointType.TCP, command, "") shouldBe Unit.right()
    }

    @Test
    fun `test connection rejects text that is not a redis address`() = runTest {
        val result = repository.testConnection(SyncEndpointType.TCP, "https://db.upstash.io", "t")

        result.shouldBeInstanceOf<Either.Left<String>>()
        coVerify(exactly = 0) { tcp.ping(any(), any()) }
    }

    @Test
    fun `test connection pings a valid https url`() = runTest {
        coEvery { rest.ping("https://db.upstash.io", "t") } returns Unit.right()

        repository.testConnection(SyncEndpointType.HTTPS, "https://db.upstash.io/", " t ") shouldBe
            Unit.right()
    }
    // endregion

    private fun meta(deviceId: String, updatedAt: Long) = RemoteSyncMeta(
        deviceId = deviceId,
        updatedAt = updatedAt,
        accounts = 1,
        appVersion = "test",
    )
}
