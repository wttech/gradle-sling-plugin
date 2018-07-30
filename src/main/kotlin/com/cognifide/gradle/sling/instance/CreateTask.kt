package com.cognifide.gradle.sling.instance

import com.cognifide.gradle.sling.api.SlingDefaultTask
import com.cognifide.gradle.sling.api.SlingTask
import com.cognifide.gradle.sling.internal.file.resolver.FileResolver
import groovy.lang.Closure
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.util.ConfigureUtil
import java.io.File

open class CreateTask : SlingDefaultTask() {

    companion object {
        const val NAME = "slingCreate"

        const val DOWNLOAD_DIR = "download"

        const val JAR_URL_PROP = "sling.instance.local.jarUrl"

        const val JAR_URL_DEFAULT = "https://github.com/Cognifide/gradle-sling-plugin/releases/download/downloads/org.apache.sling.starter.jar"
    }

    @Internal
    val instanceFileResolver = FileResolver(project, SlingTask.temporaryDir(project, NAME, DOWNLOAD_DIR))

    @get:Internal
    val instanceFiles: List<File>
        get() = instanceFileResolver.allFiles()

    init {
        description = "Creates local Sling instance(s)."

        instanceFilesByProperties()
    }

    private fun instanceFilesByProperties() {
        val jarUrl = props.string(JAR_URL_PROP, JAR_URL_DEFAULT)
        if (!jarUrl.isBlank()) {
            instanceFileResolver.url(jarUrl)
        }
    }

    fun instanceFiles(closure: Closure<*>) {
        ConfigureUtil.configure(closure, instanceFileResolver)
    }

    @TaskAction
    fun create() {
        val handles = Instance.handles(project).filter { !it.created }
        if (handles.isEmpty()) {
            logger.info("No instances to create")
            return
        }

        logger.info("Creating instances")
        handles.parallelStream().forEach { it.create(instanceFiles) }

        notifier.default("Instance(s) created", "Which: ${handles.names}")
    }

}