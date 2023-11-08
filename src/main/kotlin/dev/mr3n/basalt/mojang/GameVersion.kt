package dev.mr3n.basalt.mojang

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class GameVersion {
    @SerialName("old_alpha")
    OLD_ALPHA,
    @SerialName("old_beta")
    OLD_BETA,
    @SerialName("snapshot")
    SNAPSHOT,
    @SerialName("release")
    RELEASE;
}