package dev.mr3n.basalt.mojang

import dev.mr3n.basalt.HttpRequest
import dev.mr3n.basalt.JsonParser
import dev.mr3n.basalt.constant.URLs
import io.ktor.client.call.*
import io.ktor.client.network.sockets.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlin.jvm.Throws
import kotlin.time.Duration.Companion.days

object MojangLauncherAPI {
    @Throws(ConnectTimeoutException::class)
    suspend fun versionManifest(): VersionManifestJson {
        val response = HttpRequest.get(URLs.VERSION_MANIFEST_V2, caching = true, cacheExpiresAfter = 1.days)
        return JsonParser.decode(response)
    }
}