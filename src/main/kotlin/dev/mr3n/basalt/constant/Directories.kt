package dev.mr3n.basalt.constant

import java.io.File

object Directories {
    val BASE = File("")

    val INSTANCES = BASE.resolve("instances")

    val MANAGED_JAVAS = BASE.resolve("managed_javas")

    val LIBRARIES = BASE.resolve("libraries")
}