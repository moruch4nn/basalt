package dev.mr3n.basalt.java

import dev.mr3n.basalt.HttpRequest
import dev.mr3n.basalt.JsonParser
import dev.mr3n.basalt.constant.Directories
import dev.mr3n.basalt.database.Database
import dev.mr3n.basalt.exception.ArchNotSupportedException
import dev.mr3n.basalt.exception.OSNotSupportedException
import io.ktor.client.call.*
import io.ktor.client.network.sockets.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.cio.*
import io.ktor.util.network.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import org.apache.commons.lang3.ArchUtils
import org.apache.commons.lang3.SystemUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import java.io.File
import java.net.SocketException
import java.nio.file.Files
import java.util.*
import kotlin.io.path.absolutePathString
import kotlin.time.Duration.Companion.minutes

object JavaUtils {

    private const val JAVA_INFO_JSON = "java_info.json"

    fun managedJavaList(baseDir: File = Directories.MANAGED_JAVAS): List<ManagedJavaInfo> {
        return baseDir.listFiles()?.map { it.resolve(JAVA_INFO_JSON) }?.map { JsonParser.decode(it.readBytes()) }?: listOf()
    }

    /**
     * Download the Java binary file and place it appropriately in the folder.
     *
     * @param metadata azul zulu metadata
     * @param baseDir Folder to save java binary files
     */
    @Throws(ConnectTimeoutException::class, UnresolvedAddressException::class, SocketException::class, IllegalStateException::class, ZipException::class, RedirectResponseException::class, ClientRequestException::class, ServerResponseException::class)
    suspend fun install(metadata: AzulZuluMetadata, baseDir: File = Directories.MANAGED_JAVAS, force: Boolean = false): ManagedJavaInfo {
        val javaName = metadata.name.removeSuffix(".zip")
        val dir = baseDir.resolve(javaName).absoluteFile

        val managedJavaInfo = ManagedJavaInfo(
            name = "java${metadata.majorVersion()}-azul_zulu-${metadata.version()}",
            path = dir.absolutePath,
            vendor = ManagedJavaInfo.Vendor.AZUL_ZULU,
            version = metadata.version(),
            majorVersion = metadata.majorVersion(),
            downloadUrl = metadata.downloadUrl
        )

        if(dir.exists()) {
            // if:すでに指定バージョンのjavaがインストールされている場合
            if(force) { dir.deleteRecursively() } else { return managedJavaInfo }
        } else {
            dir.mkdirs()
        }
        // ダウンロードしたjava
        val response = HttpRequest.get(metadata.downloadUrl, caching = false)
        // 一時的なzipファイル
        val archivePath = withContext(Dispatchers.IO) { Files.createTempFile("${UUID.randomUUID()}", ".tmp") }

        val archiveFile = archivePath.toFile()
        // 下でエラーが発生した場合でも正常にファイルが削除されるようにする
        archiveFile.deleteOnExit()
        // 一時的にzipファイルに保存
        archiveFile.writeBytes(response)
        // 一応100ミリ秒ほど待つ
        delay(100)
        // javaのbinaryが入ったzipファイルをすべて解凍する
        ZipFile(archiveFile).extractAll(baseDir.absolutePath)
        // 解凍後にファイルを削除
        archiveFile.delete()

        // データベースにインストールしたJavaの詳細を格納する
        dir.resolve(JAVA_INFO_JSON).writeText(JsonParser.encode(managedJavaInfo))

        return managedJavaInfo
    }

    @Throws(OSNotSupportedException::class, ArchNotSupportedException::class, RedirectResponseException::class, ClientRequestException::class, ServerResponseException::class)
    suspend fun availableJREs(majorVersion: Int? = null, latest: Boolean = true): List<AzulZuluMetadata> {
        val os = when {
            SystemUtils.IS_OS_WINDOWS -> "windows"
            SystemUtils.IS_OS_MAC -> "mac"
            SystemUtils.IS_OS_LINUX -> "linux"
            else -> throw OSNotSupportedException(SystemUtils.OS_NAME)
        }
        val processor = ArchUtils.getProcessor()
        val arch = when {
            processor.isAarch64 -> "aarch64"
            processor.isPPC -> "ppc"
            processor.is64Bit -> "x64"
            processor.is32Bit -> "x86"
            else -> throw ArchNotSupportedException(processor.arch.name)
        }
        var url = "https://api.azul.com/metadata/v1/zulu/packages?os=${os}&arch=${arch}&archive_type=zip&java_package_type=jre"
        if(majorVersion != null) { url += "&java_version=${majorVersion}" }
        if(latest) { url += "&latest=true" }
        return JsonParser.decode(HttpRequest.get(url, caching = true, cacheExpiresAfter = 10.minutes))
    }
}