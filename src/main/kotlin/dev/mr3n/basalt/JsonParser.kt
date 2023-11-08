package dev.mr3n.basalt

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object JsonParser {
    val parser = Json {
        ignoreUnknownKeys = true
    }

    inline fun <reified T: Any> encode(instance: T): String = this.parser.encodeToString(instance)

    inline fun <reified T> decode(byteArray: ByteArray): T = this.parser.decodeFromString(byteArray.decodeToString())
}