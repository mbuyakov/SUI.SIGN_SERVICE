package ru.sui.signservice.configuration

import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipInputStream
import javax.annotation.PostConstruct
import kotlin.io.path.exists

private val log = KotlinLogging.logger { }

@Configuration
class KeyStoreConfiguration {

    @PostConstruct
    fun initHDImageStoreFromEnvironment() {
        val cryptoproKeysArchive = System.getenv("CRYPTOPRO_KEYS_ARCHIVE")
            ?.takeIf { it.isNotBlank() }
            ?: "/opt/keystorevolume/archive"

        if (!Paths.get(cryptoproKeysArchive).exists()) {
            log.warn { "$cryptoproKeysArchive doesn't exist" }
            return
        }

        // unarchive new key in keystore
        val cryptoproKeysDir = System.getenv("CRYPTOPRO_KEYS_DIR")
            ?.takeIf { it.isNotBlank() }
            ?: ("/var/opt/cprocsp/keys/" + System.getProperty("user.name"))

        try {
            log.info { "Unzip $cryptoproKeysArchive to $cryptoproKeysDir" }

            File(cryptoproKeysArchive).inputStream().use { archiveInputStream ->
                ZipInputStream(archiveInputStream).use { zipInputStream ->
                    while (true) {
                        val entry = zipInputStream.nextEntry ?: break
                        val path = Paths.get(cryptoproKeysDir).resolve(entry.name)

                        if (path.exists()) {
                            log.info { " already exists: $path" }
                        } else if (entry.isDirectory) {
                            log.info { " create dir: $path" }
                            Files.createDirectories(path)
                        } else {
                            if (!path.parent.exists()) {
                                log.info(" create sub dir: ${path.parent}")
                                Files.createDirectories(path.parent)
                            }

                            log.info { " save file: $path" }
                            Files.copy(zipInputStream, path)
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            log.error(exception) { "Can't init keystore: " + exception.message }
        }
    }

}