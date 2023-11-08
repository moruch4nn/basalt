import dev.mr3n.basalt.database.Database
import dev.mr3n.basalt.java.JavaUtils
import dev.mr3n.basalt.mojang.MojangLauncherAPI
import kotlinx.coroutines.delay

suspend fun main(args: Array<String>) {
    Database.init()
    val versionManifest = MojangLauncherAPI.versionManifest()
    val version = versionManifest.versions.find { it.id == versionManifest.latest.release }?:return
    val metadata = version.metadata()
    metadata.libraries.forEach { library ->
        val start = System.currentTimeMillis()
        if(library.hasArtifact()) {
            if(library.exists(checkDigest = true)) { return@forEach }
            println("downloading ${library.name}...")
            library.download()
            println("downloaded ${library.name}! (${System.currentTimeMillis() - start}ms)")
        }
    }

    JavaUtils.install(JavaUtils.availableJREs(majorVersion = 17, latest = true).first())
    delay(1000)
}