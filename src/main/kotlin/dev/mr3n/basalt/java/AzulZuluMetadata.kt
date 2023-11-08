package dev.mr3n.basalt.java

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AzulZuluMetadata(
    @SerialName("availability_type")
    val availabilityType: String,
    @SerialName("distro_version")
    val distroVersion: List<Int>,
    @SerialName("download_url")
    val downloadUrl: String,
    @SerialName("java_version")
    val javaVersion: List<Int>,
    val latest: Boolean,
    val name: String,
    @SerialName("openjdk_build_number")
    val openjdkBuildNumber: Int,
    @SerialName("package_uuid")
    val packageUUID: String,
    val product: String
) {
    fun majorVersion(): Int = this.javaVersion[0]

    fun version(): String = this.javaVersion.joinToString(".")
}