package com.ivy.data.sync

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SyncConfigDataSourceTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dataSource by lazy {
        SyncConfigDataSource(
            PreferenceDataStoreFactory.create(scope = scope) {
                folder.newFile("sync.preferences_pb")
            }
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `a change made during an upload stays marked after that upload`() = runTest {
        // The upload read the database at 150; the change at 200 is not in it.
        dataSource.markLocalChange(at = 100L)
        dataSource.markLocalChange(at = 200L)

        dataSource.clearLocalChange(since = 150L)

        dataSource.get().localChangedAt shouldBe 200L
    }

    @Test
    fun `changes are forgotten once an upload that includes them finishes`() = runTest {
        dataSource.markLocalChange(at = 100L)
        dataSource.markLocalChange(at = 200L)

        dataSource.clearLocalChange(since = 250L)

        dataSource.get().localChangedAt shouldBe 0L
    }

    @Test
    fun `removing the connection forgets the sync state`() = runTest {
        dataSource.setConnection("rediss://db.upstash.io:6379", "pw", SyncEndpointType.TCP)
        dataSource.setMode(SyncMode.AUTO)
        dataSource.setLastSyncedUpdatedAt(5L)
        dataSource.markLocalChange(at = 6L)

        dataSource.clearConnection()

        val config = dataSource.get()
        config.isConfigured shouldBe false
        config.mode shouldBe SyncMode.OFF
        config.lastSyncedUpdatedAt shouldBe 0L
        config.localChangedAt shouldBe 0L
        config.deviceId.isNotBlank() shouldBe true
    }

    @Test
    fun `switching to another database forgets the old database's sync position`() = runTest {
        dataSource.setConnection("rediss://old.upstash.io:6379", "pw", SyncEndpointType.TCP)
        dataSource.setLastSyncedUpdatedAt(5L)
        dataSource.markLocalChange(at = 6L)

        dataSource.setConnection("rediss://new.upstash.io:6379", "pw", SyncEndpointType.TCP)

        dataSource.get().lastSyncedUpdatedAt shouldBe 0L
        dataSource.get().localChangedAt shouldBe 0L
    }

    @Test
    fun `saving the same database again keeps the sync position`() = runTest {
        dataSource.setConnection("rediss://db.upstash.io:6379", "pw", SyncEndpointType.TCP)
        dataSource.setLastSyncedUpdatedAt(5L)

        dataSource.setConnection("rediss://db.upstash.io:6379/", " pw ", SyncEndpointType.TCP)

        dataSource.get().lastSyncedUpdatedAt shouldBe 5L
    }
}
