package dev.mr3n.basalt.mojang

import dev.mr3n.basalt.HttpRequest
import dev.mr3n.basalt.constant.Directories
import dev.mr3n.basalt.exception.OSNotSupportedException
import io.ktor.client.network.sockets.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.cio.*
import io.ktor.util.network.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.ArchUtils
import org.apache.commons.lang3.SystemUtils
import java.io.File
import java.io.IOException
import java.net.SocketException
import java.nio.file.FileAlreadyExistsException

/**
 * serialize class for https://minecraft.fandom.com/wiki/Client.json
 */
@Serializable
data class ClientMetadataJson(
    // asset info.
    val assetIndex: AssetIndex,
    // The assets version.
    val assets: String,
    // Its value is 1 for all recent versions of the game (1.16.4 and above) or 0 for all others. This tag tells the launcher whether it should urge the user to be careful since this version is older and might not support the latest player safety features.
    val complianceLevel: Int,
    // Contains download links for files necessary for execution, such as client.jar and server.jar.
    val downloads: Map<String, Download>,
    // A list of libraries.
    val libraries: List<Library>,
    // The name of this version client (e.g. 1.14.4).
    val id: String,
    // The version of the Java Runtime Environment.
    val javaVersion: JavaVersion,
    // The main game class
    val mainClass: String,
    // The type of this game version. It is shown in the version list when you create a new installation. The default values are "release" and "snapshot".
    val type: GameVersion,
    // The release date and time.
    val releaseTime: Instant
) {
    @Serializable
    data class JavaVersion(
        // Its value for all 1.17 snapshots is "jre-legacy" until 21w18a and "java-runtime-alpha" since 21w19a.
        val component: String,
        // Its value for all 1.17 snapshots is 8 until 21w18a and 16 since 21w19a.
        val majorVersion: Int
    )

    @Serializable
    data class Library(
        val name: String,
        val downloads: Download,
        val rules: List<Rule>? = null,
        val extract: Extract? = null,
        val natives: Map<NativeClassifier, String>? = null,
    ) {

        fun exists(dir: File = Directories.LIBRARIES, checkDigest: Boolean = false): Boolean {
            val artifact = checkNotNull(this.artifact()) { "artifact not found." }
            val file = dir.resolve(artifact.path)
            // ファイルが存在しない場合はreturn false
            if(!file.exists()) { return false }
            // digestを確認しない場合はファイルが存在することを確認済みのためreturn true
            if(!checkDigest) { return true }
            // sha1ハッシュが一致するかどうかを確認
            return artifact.sha1 == DigestUtils.sha1Hex(file.inputStream())
        }

        fun hasArtifact() = this.artifact() != null

        /**
         * Download the library to the specified location.
         * Specify the original libraries directory to automatically create the path destination directory.
         *
         * @param dir Specify libraries directory
         */
        @Throws(ConnectTimeoutException::class, UnresolvedAddressException::class, SocketException::class, IllegalStateException::class, IOException::class, FileAlreadyExistsException::class, RedirectResponseException::class, ClientRequestException::class, ServerResponseException::class, HttpRequestTimeoutException::class)
        suspend fun download(dir: File = Directories.LIBRARIES) {
            val artifact = checkNotNull(this.artifact()) { "artifact not found." }
            val file = dir.resolve(artifact.path)
            if(file.exists()) { throw FileAlreadyExistsException("artifact already exists.") }
            file.parentFile.mkdirs()
            val response = HttpRequest.get(artifact.url, caching = false)
            file.writeBytes(response)
            // ダウンロードしたファイルと想定ファイルサイズが一致しない場合はエラー
            if(response.size != artifact.size) {
                file.delete()
                throw IllegalStateException("sha1 not matched.")
            }
        }

        private var artifact: Download.Artifact? = null

        /**
         * Obtain an Artifact instance according to the environment of the computer on which it is running
         *
         * @return If null, there was no artifact suitable for this environment.
         */
        fun artifact(refresh: Boolean = false): Download.Artifact? {

            if(!refresh && this.artifact != null) { return artifact }

            // downloadsからartifactを取得し、nullの場合はclassifiersからこの環境に適したartifactを取得、それも存在しない場合はnullをreturnする
            //     {
            //      "downloads": {
            //        "artifact": { <========== ここ！
            var artifact: Download.Artifact? = this.downloads.artifact

            // artifactがnullだった場合はclassifier内の情報を確認する
            if(artifact == null) {
                //    {
                //      "downloads": {
                //        "classifiers": { <========== ここ！
                //          "natives-linux": {

                // nativeOsの種類(windows,macos,linux等)を取得
                val nativeOs = NativeClassifier.native()
                val processor = ArchUtils.getProcessor()
                val nativeName = this.natives?.get(nativeOs)?.replace("\${arch}", if(processor.is32Bit) "32" else "64")
                artifact = this.downloads.classifiers?.get(nativeName)
            }

            // rulesのための変数。すべてのruleを確認し、最終的にfalseの場合はnullをreturnする。
            val rules = this.rules
            //
            val result = if(rules != null) {
                // if:rulesがnullじゃない場合はrulesの内容を確認し対応していない場合はnullを返す
                var allow = false
                // すべてのruleを確認しallow変数の内容を変更する
                this.rules?.forEach rule@{ rule -> if(rule.isMatched()) { allow = rule.action.bool } }
                // allowがfalseだった場合はnullを返す
                if(allow) { artifact } else { null }
            } else {
                // if:rulesがnullの場合はartifactを返す
                artifact
            }

            this.artifact = result

            return result
        }

        enum class NativeClassifier {
            @SerialName("linux")
            LINUX,

            @SerialName("osx")
            OSX,

            @SerialName("windows")
            WINDOWS;

            companion object {
                @Throws(OSNotSupportedException::class)
                fun native(): NativeClassifier {
                    return when {
                        SystemUtils.IS_OS_LINUX -> LINUX
                        SystemUtils.IS_OS_MAC -> OSX
                        SystemUtils.IS_OS_WINDOWS -> WINDOWS
                        else -> throw OSNotSupportedException()
                    }
                }
            }
        }

        @Serializable
        data class Extract(
            val exclude: List<String>
        )

        @Serializable
        data class Rule(
            val action: Action,
            val os: OperatingSystem? = null
        ) {

            fun isMatched(): Boolean {
                val nativeOs = NativeClassifier.native()
                val nativeOsVersion = System.getProperty("os.version")
                val nativeArch = ArchUtils.getProcessor().type.name.lowercase()
                // ruleとos名が一致しない場合はcontinueする
                if((this.os?.name?:nativeOs) != nativeOs) { return false }
                // ruleとosのversionが一致しない場合はcontinueする
                if(!Regex(this.os?.version?:".*?").matches(nativeOsVersion)) { return false }
                if((this.os?.arch?:nativeArch) != nativeArch) { return false }
                return true
            }

            @Serializable
            data class OperatingSystem(
                val name: NativeClassifier? = null,
                val version: String? = null,
                val arch: String? = null
            )

            @Serializable
            enum class Action(val bool: Boolean) {
                @SerialName("allow")
                ALLOW(true),

                @SerialName("disallow")
                DISALLOW(false)
            }
        }

        @Serializable
        data class Download(
            val artifact: Artifact? = null,
            val classifiers: Map<String, Artifact>? = null
        ) {
            @Serializable
            data class Artifact(
                val path: String,
                val sha1: String,
                val size: Int,
                val url: String
            )
        }
    }

    @Serializable
    data class Download(
        val sha1: String,
        val size: Int,
        val url: String
    )

    @Serializable
    data class AssetIndex(
        val id: String,
        val sha1: String,
        val url: String
    )
}