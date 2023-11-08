package dev.mr3n.basalt.mojang

import dev.mr3n.basalt.HttpRequest
import dev.mr3n.basalt.JsonParser
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.jvm.Throws

/**
 *
 * Serialize for version_manifest_v2.json
 *
 * URL: https://launchermeta.mojang.com/mc/game/version_manifest_v2.json
 *
 * INFO: https://minecraft.fandom.com/en/wiki/Version_manifest.json
 */
@Serializable
data class VersionManifestJson(
    val latest: Latest,
    val versions: List<Version>
) {
    @Serializable
    data class Latest(
        // The ID of the latest release version.
        val release: String,
        // The ID of the latest snapshot version.
        val snapshot: String
    )

    @Serializable
    data class Version(
        // The ID of this version.
        val id: String,
        // The type of this version.
        val type: GameVersion,
        // The link to the <version id>.json for this version.
        val url: String,
        // A timestamp in ISO 8601 format of when the version files were last updated on the manifest.
        val time: Instant,
        // The release time of this version in ISO 8601 format.
        val releaseTime: Instant,
        // The SHA1 hash of the version and therefore the JSON file ID.
        val sha1: String,
        // If 0, the launcher warns the user about this version not being recent enough to support the latest player safety features. Its value is 1 otherwise.
        val complianceLevel: Int
    ) {
        @Throws(IllegalStateException::class, RedirectResponseException::class, ClientRequestException::class, ServerResponseException::class)
        suspend fun metadata(): ClientMetadataJson {
            val response = HttpRequest.get(this.url, caching = true, cacheExpiresAfter = null)
            return JsonParser.decode(response)
        }
    }
}