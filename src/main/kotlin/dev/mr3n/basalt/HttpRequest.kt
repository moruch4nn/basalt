package dev.mr3n.basalt

import dev.mr3n.basalt.database.Database
import dev.mr3n.basalt.database.Database.HttpResponseCacheTable.expiresAt
import dev.mr3n.basalt.database.Database.HttpResponseCacheTable.response
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.upsert
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import dev.mr3n.basalt.database.Database.HttpResponseCacheTable.url
import org.jetbrains.exposed.sql.transactions.transaction

object HttpRequest {
    private val httpClient = HttpClient(CIO) {
        expectSuccess = true
        install(HttpTimeout) {
            connectTimeoutMillis = 10000L
            requestTimeoutMillis = 10000L
            socketTimeoutMillis = 10000L
        }
        install(ContentNegotiation) {
            json(JsonParser.parser)
        }
    }

    /**
     * get request.
     *
     * @param requestUrl request url. e.q. google.com/test
     * @param caching Whether to cache request results
     * @param cacheExpiresAfter When the cache expires
     */
    suspend fun get(requestUrl: String, headers: Map<String, Any?> = mapOf(), caching: Boolean, cacheExpiresAfter: Duration? = 10.days): ByteArray {

        if(caching) {
            val cachedResponse = transaction {
                 Database.HttpResponseCacheTable.select {
                    (url eq requestUrl) and ((expiresAt eq null) or (expiresAt greater Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())))
                }.firstOrNull() }
            if(cachedResponse != null) { return cachedResponse[response].bytes }
        }

        val response = this.httpClient.get(requestUrl) {
            headers.forEach { (key, v) -> header(key, v) }
        }
        val channel = response.bodyAsChannel()
        val byteArray = channel.toByteArray()

        if(caching) {
            transaction {
                Database.HttpResponseCacheTable.upsert {
                    it[url] = requestUrl
                    if(cacheExpiresAfter != null) { it[expiresAt] = Clock.System.now().plus(cacheExpiresAfter).toLocalDateTime(TimeZone.currentSystemDefault()) }
                    it[this.response] = ExposedBlob(byteArray)
                }
            }
        }

        return byteArray
    }
}