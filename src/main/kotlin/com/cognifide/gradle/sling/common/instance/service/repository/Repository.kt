package com.cognifide.gradle.sling.common.instance.service.repository

import com.cognifide.gradle.sling.common.instance.InstanceService
import com.cognifide.gradle.sling.common.instance.InstanceSync
import java.io.File

class Repository(sync: InstanceSync) : InstanceService(sync) {

    /**
     * Take care about property value types saved in repository.
     */
    val typeHints = sling.obj.boolean {
        convention(true)
        sling.prop.boolean("instance.repository.typeHints")?.let { set(it) }
    }

    /**
     * Controls level of logging. By default repository related operations are only logged at debug level.
     * This switch could increase logging level to info level.
     */
    val verboseLogging = sling.obj.boolean {
        convention(false)
        sling.prop.boolean("instance.repository.verboseLogging")?.let { set(it) }
    }

    /**
     * Controls throwing exceptions in case of response statuses indicating repository errors.
     * Switching it to false, allows custom error handling in task scripting.
     */
    val responseChecks = sling.obj.boolean {
        convention(true)
        sling.prop.boolean("instance.repository.responseChecks")?.let { set(it) }
    }

    /**
     * Get node at given path.
     */
    fun node(path: String) = Node(this, path)

    /**
     * Get node at given path and perform action in its scope (and optionally return result).
     */
    fun <T> node(path: String, action: Node.() -> T): T = node(path).run(action)

     /**
     * Shorthand method for creating or updating node at given path.
     */
    fun save(path: String, properties: Map<String, Any?>): RepositoryResult {
         val (dir, name) = splitPath(path)
         return when {
             name.contains(".") -> node(dir).import(properties, name, replace = true, replaceProperties = true)
             else -> node(path).save(properties)
         }
    }

    /**
     * Shorthand method for importing content from JSON file at given path.
     */
    fun import(path: String, jsonFile: File): RepositoryResult {
        val (dir, name) = splitPath(path)
        return node(dir).import(jsonFile, name, replace = true, replaceProperties = true)
    }

    private fun splitPath(path: String): Pair<String, String> {
        return path.substringBeforeLast("/") to path.substringAfterLast("/")
    }

    internal fun log(message: String, e: Throwable? = null) = when {
        verboseLogging.get() -> logger.info(message, e)
        else -> logger.debug(message, e)
    }

    internal val http by lazy {
        RepositoryHttpClient(sling, instance).apply {
            responseChecks.set(this@Repository.responseChecks)
        }
    }
}
