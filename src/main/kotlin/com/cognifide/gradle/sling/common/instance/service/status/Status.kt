package com.cognifide.gradle.sling.common.instance.service.status

import com.cognifide.gradle.sling.common.instance.InstanceService
import com.cognifide.gradle.sling.common.instance.InstanceSync
import com.cognifide.gradle.common.CommonException
import com.cognifide.gradle.common.utils.Patterns
import java.util.*

/**
 * Allows to read statuses available at Apache Felix Web Console.
 *
 * @see <https://felix.apache.org/documentation/subprojects/apache-felix-web-console.html>
 */
class Status(sync: InstanceSync) : InstanceService(sync) {

    /**
     * System properties of instance read once across whole build, fail-safe.
     */
    val systemProperties: Map<String, String> get() = readPropertiesOnce(SYSTEM_PROPERTIES_PATH)

    /**
     * Sling setting of instance read once across whole build, fail-safe.
     */
    val slingSettings: Map<String, String> get() = readPropertiesOnce(SLING_SETTINGS_PATH)

    /**
     * Sling properties of instance read once across whole build, fail-safe.
     */
    val slingProperties: Map<String, String> get() = readPropertiesOnce(SLING_PROPERTIES_PATH)

    fun readPropertiesOnce(path: String): Map<String, String> {
        if (sling.commonOptions.offline.get()) {
            return mapOf()
        }

        return common.buildScope.tryGetOrPut("${instance.httpUrl}$path") {
            try {
                readProperties(path).apply {
                    sling.logger.info("Successfully read status properties at path '$path' on $instance.")
                }
            } catch (e: StatusException) {
                sling.logger.debug("Failed when reading status properties at path '$path' on $instance!", e)
                null
            }
        } ?: mapOf()
    }

    /**
     * Read system properties like server timezone & encoding, Java version, OS details.
     */
    @Suppress("unchecked_cast")
    fun readProperties(path: String): Map<String, String> = try {
        sync.http.get(path) {
            Properties().apply { load(statusPropertiesAsIni(asString(it))) } as Map<String, String>
        }
    } catch (e: CommonException) {
        throw StatusException("Cannot read status properties for path '$path' on $instance! Cause: ${e.message}", e)
    }

    /**
     * Status properties endpoints response is not valid INI file, because is not escaping
     * Windows paths with backslash. Below code is fixing 'Malformed \uxxxx encoding.' exception.
     */
    private fun statusPropertiesAsIni(text: String) = text.lineSequence()
            .filter { Patterns.wildcard(it, "* = *") } // Filter headings starting with '*** '
            .map {
                val key = it.substringBefore("=").trim().replace(" ", "_") // Spaces in keys fix
                val value = it.substringAfter("=").trim().replace("\\", "\\\\") // Windows paths fix
                "$key=$value"
            }
            .joinToString("\n")
            .byteInputStream()

    companion object {
        const val SYSTEM_PROPERTIES_PATH = "/system/console/status-System Properties.txt"

        const val SLING_SETTINGS_PATH = "/system/console/status-slingsettings.txt"

        const val SLING_PROPERTIES_PATH = "/system/console/status-slingprops.txt"
    }
}
