package dev.mr3n.basalt.java

import kotlinx.serialization.Serializable
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists

@Serializable
data class ManagedJavaInfo(
    val name: String,
    val majorVersion: Int,
    val version: String,
    val path: String,
    val vendor: Vendor,
    val downloadUrl: String,
) {
    /**
     * Whether the installed Java is still present
     */
    fun exists(): Boolean = File(this.path).resolve("bin").exists()

    enum class Vendor {
        AZUL_ZULU
    }
}