package com.ivy.data.sync.impl

import arrow.core.Either
import com.ivy.base.TestDispatchersProvider
import com.ivy.base.di.KotlinxSerializationModule
import com.ivy.data.sync.CloudBackupCodec
import com.ivy.data.sync.RedisAddress
import com.ivy.data.sync.RedisConnectionString
import com.ivy.data.sync.RedisSyncDataSource
import com.ivy.data.sync.RedisSyncDataSource.Companion.BACKUP_KEY
import com.ivy.data.sync.RedisSyncDataSource.Companion.META_KEY
import com.ivy.data.sync.model.RemoteSyncMeta
import com.ivy.data.testResource
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.net.InetAddress

/**
 * Runs both Redis clients against a real Upstash database. Skipped unless the environment variable
 * `IVY_TEST_REDIS_URL` holds a connection string with a password, e.g. the console's
 * `redis-cli --tls -u redis://default:PASSWORD@host:6379` command:
 *
 *   IVY_TEST_REDIS_URL='redis://default:…@….upstash.io:6379' ./gradlew :shared:data:core:testDebugUnitTest
 *
 * The test writes the backup keys and restores their previous values afterwards.
 */
class RedisLiveIntegrationTest {
    private val json = KotlinxSerializationModule.provideJson()
    private lateinit var address: RedisAddress
    private lateinit var http: HttpClient
    private lateinit var rest: RedisSyncDataSourceImpl
    private lateinit var tcp: RedisTcpSyncDataSourceImpl
    private var savedBackup: String? = null
    private var savedMeta: String? = null

    private val restUrl get() = address.restUrl
    private val tcpUrl get() = address.tlsUrl
    private val password get() = address.password!!

    @Before
    fun setup() {
        val connection = System.getenv("IVY_TEST_REDIS_URL")
        assumeTrue("IVY_TEST_REDIS_URL is not set", !connection.isNullOrBlank())
        address = requireNotNull(RedisConnectionString.parse(connection!!)?.takeIf { it.password != null })
        http = HttpClient(OkHttp) { install(ContentNegotiation) { json(json) } }
        rest = RedisSyncDataSourceImpl({ http }, json)
        tcp = RedisTcpSyncDataSourceImpl(json, TestDispatchersProvider)
        runBlocking {
            savedBackup = rawGet(BACKUP_KEY)
            savedMeta = rawGet(META_KEY)
        }
    }

    @After
    fun restore() = runBlocking<Unit> {
        if (!::tcp.isInitialized) return@runBlocking
        val backup = savedBackup
        val meta = savedMeta
        rawDelete(BACKUP_KEY, META_KEY)
        if (backup != null) rawSet(BACKUP_KEY, backup)
        if (meta != null) rawSet(META_KEY, meta)
        http.close()
    }

    @Test
    fun `both clients connect with the pasted redis-cli command`() = runBlocking<Unit> {
        val command = "redis-cli --tls -u redis://default:$password@${address.host}:${address.port}"

        tcp.ping(command, "") shouldBe Either.Right(Unit)
        tcp.ping(tcpUrl, password) shouldBe Either.Right(Unit)
        rest.ping(restUrl, password) shouldBe Either.Right(Unit)
    }

    @Test
    fun `a backup written over one connection type reads back over the other`() = runBlocking<Unit> {
        val backup = testResource("backups/m3-v1.0.1-cloud.json").readText()
        val meta = RemoteSyncMeta("live-test", 42L, 2, "test")

        rest.putBackup(restUrl, password, backup, meta).getOrThrow()
        tcp.getSnapshot(tcpUrl, password).getOrThrow().let {
            it.storedBackup shouldBe backup
            it.meta shouldBe meta
        }

        tcp.putBackup(tcpUrl, password, backup, meta.copy(updatedAt = 43L)).getOrThrow()
        rest.getSnapshot(restUrl, password).getOrThrow().meta?.updatedAt shouldBe 43L
        rest.getMeta(restUrl, password).getOrThrow()?.updatedAt shouldBe 43L
    }

    @Test
    fun `backups written by v1_0_1 and v1_0_8 read back unchanged`() = runBlocking<Unit> {
        listOf("m3-v1.0.1-cloud", "m3-v1.0.8-cloud").forEach { name ->
            // Stored exactly as those versions wrote them: plain JSON, compact meta.
            rawSet(BACKUP_KEY, testResource("backups/$name.json").readText())
            rawSet(META_KEY, testResource("backups/$name-meta.json").readText())

            listOf(rest to restUrl, tcp to tcpUrl).forEach { (client, url) ->
                val snapshot = (client as RedisSyncDataSource).getSnapshot(url, password).getOrThrow()
                CloudBackupCodec.decode(snapshot.storedBackup!!) shouldBe
                    testResource("backups/$name.json").readText()
                snapshot.meta?.accounts shouldBe 2
            }
        }
    }

    @Test
    fun `a backup without its meta record is reported, not hidden`() = runBlocking<Unit> {
        rawDelete(META_KEY)
        rawSet(BACKUP_KEY, "{}")

        rest.getMeta(restUrl, password).getOrThrow() shouldBe RemoteSyncMeta.unknown()
        tcp.getMeta(tcpUrl, password).getOrThrow() shouldBe RemoteSyncMeta.unknown()
        tcp.getSnapshot(tcpUrl, password).getOrThrow().meta shouldBe RemoteSyncMeta.unknown()

        rawDelete(BACKUP_KEY)
        rest.getMeta(restUrl, password).getOrThrow() shouldBe null
        tcp.getMeta(tcpUrl, password).getOrThrow() shouldBe null
    }

    @Test
    fun `a backup over Upstash's 10 MB request limit uploads once compressed`() = runBlocking<Unit> {
        val transaction = """{"accountId":"11f0e312-c29d-4ab8-a75e-3d5d643117c3","type":"EXPENSE",""" +
            """"amount":12.5,"title":"Coffee","dateTime":1791489704810,"id":"%s"},"""
        val big = "{\"transactions\":[" +
            (0 until 80_000).joinToString("") { transaction.format("id-$it") }.trimEnd(',') + "]}"
        check(big.length > 10 * 1024 * 1024)
        val meta = RemoteSyncMeta("live-test", 44L, 1, "test")

        // Uncompressed it is rejected with Upstash's own explanation.
        rest.putBackup(restUrl, password, big, meta).shouldBeInstanceOf<Either.Left<String>>()
            .value shouldContain "max request size"

        listOf(rest to restUrl, tcp to tcpUrl).forEach { (client, url) ->
            val source = client as RedisSyncDataSource
            source.putBackup(url, password, CloudBackupCodec.encode(big), meta).getOrThrow()
            CloudBackupCodec.decode(source.getSnapshot(url, password).getOrThrow().storedBackup!!) shouldBe big
        }
    }

    @Test
    fun `wrong passwords are reported, not thrown`() = runBlocking<Unit> {
        rest.ping(restUrl, "wrong").shouldBeInstanceOf<Either.Left<String>>()
            .value shouldContain "Unauthorized"
        tcp.ping(tcpUrl, "wrong").shouldBeInstanceOf<Either.Left<String>>()
        Unit
    }

    @Test
    fun `the TLS connection checks the certificate is for the host`() = runBlocking<Unit> {
        // Same server, reached by IP: its certificate does not name the IP, so it must be refused.
        val ip = InetAddress.getByName(address.host).hostAddress
        tcp.ping("rediss://$ip:${address.port}", password).shouldBeInstanceOf<Either.Left<String>>()
        Unit
    }

    private fun <T> Either<String, T>.getOrThrow(): T = fold({ error(it) }, { it })

    /** Runs one Redis command through the REST API's root endpoint. */
    private suspend fun command(vararg args: String): JsonElement? {
        val response = http.post(restUrl) {
            header(HttpHeaders.Authorization, "Bearer $password")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ListSerializer(String.serializer()), args.toList()))
        }
        return (json.parseToJsonElement(response.bodyAsText()) as JsonObject)["result"]
    }

    private suspend fun rawSet(key: String, value: String) {
        command("SET", key, value)
    }

    private suspend fun rawDelete(vararg keys: String) {
        command("DEL", *keys)
    }

    private suspend fun rawGet(key: String): String? =
        (command("GET", key) as? JsonPrimitive)?.takeIf { it.isString }?.content
}
