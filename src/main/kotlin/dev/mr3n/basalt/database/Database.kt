package dev.mr3n.basalt.database

import dev.mr3n.basalt.java.ManagedJavaInfo
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object Database {
    private val databaseFile = File("data.db")
    @OptIn(ExperimentalKeywordApi::class)
    private val databaseConfig = DatabaseConfig {
        preserveKeywordCasing = true
    }
    val database = Database.connect(url =  "jdbc:sqlite:${databaseFile.absolutePath}", driver = "org.sqlite.JDBC", databaseConfig = this.databaseConfig)

    object ManagedJavaTable : Table("managed_java") {
        val id = integer("id").autoIncrement()
        val name = varchar("name", 128).uniqueIndex()
        val majorVersion = integer("majorVersion")
        val version = varchar("version", 128)
        val path = varchar("path", 512)
        val vendor = enumeration("vendor", ManagedJavaInfo.Vendor::class)

        override val primaryKey: PrimaryKey = PrimaryKey(id)
    }

    object HttpResponseCacheTable : Table("http_response_cache") {
        val url = varchar("url", 512)
        val response = blob("response")
        val expiresAt = datetime("expires_at").nullable().default(null)

        override val primaryKey: PrimaryKey = PrimaryKey(url)
    }

    fun init() {
        transaction(this.database) {
            SchemaUtils.create(ManagedJavaTable, HttpResponseCacheTable)
            // 期限切れのキャッシュを削除
            HttpResponseCacheTable.deleteWhere { expiresAt less Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
        }
    }
}