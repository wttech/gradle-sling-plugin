package com.cognifide.gradle.sling.instance

import com.cognifide.gradle.sling.api.SlingConfig
import com.cognifide.gradle.sling.api.SlingException
import com.cognifide.gradle.sling.internal.Formats
import com.cognifide.gradle.sling.internal.Patterns
import com.cognifide.gradle.sling.internal.PropertyParser
import com.cognifide.gradle.sling.internal.file.FileOperations
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.internal.os.OperatingSystem
import org.gradle.util.GFileUtils
import java.io.File

class LocalHandle(val project: Project, val instance: Instance) {

    companion object {
        val JAR_NAME_PATTERNS = listOf(
                "*sling*.jar",
                "*.jar"
        )

        const val LOCK_CREATE = "create"

        const val LOCK_INIT = "init"
    }

    class Script(val script: File, val command: List<String>) {
        val commandLine: List<String>
            get() = command + listOf(script.absolutePath)

        override fun toString(): String {
            return "Script(commandLine=$commandLine)"
        }
    }

    val logger: Logger = project.logger

    val config = SlingConfig.of(project)

    val dir = File("${config.createPath}/${instance.type}")

    val jar = File(dir, "sling.jar")

    val startScript: Script
        get() = binScript("start")

    val running: Boolean
        get() = File("$dir/conf/controlport").exists()

    val stopScript: Script
        get() = binScript("stop")

    private fun binScript(name: String, os: OperatingSystem = OperatingSystem.current()): Script {
        return if (os.isWindows) {
            Script(File(dir, "$name.bat"), listOf("cmd", "/C"))
        } else {
            Script(File(dir, name), listOf("sh"))
        }
    }

    fun create(instanceFiles: List<File>) {
        if (created) {
            logger.info(("Instance already created: $this"))
            return
        }

        cleanDir(true)

        logger.info("Creating instance at path '${dir.absolutePath}'")

        logger.info("Copying resolved instance files: $instanceFiles")
        copyFiles(instanceFiles)

        logger.info("Validating instance files")
        validateFiles()

        logger.info("Creating default instance files")
        FileOperations.copyResources(InstancePlugin.FILES_PATH, dir, true)

        val filesDir = File(config.createFilesPath)

        logger.info("Overriding instance files using: ${filesDir.absolutePath}")
        if (filesDir.exists()) {
            FileUtils.copyDirectory(filesDir, dir)
        }

        logger.info("Expanding instance files")
        FileOperations.amendFiles(dir, config.createFilesExpanded, { file, source ->
            PropertyParser(project).expand(source, properties, file.absolutePath)
        })

        logger.info("Creating lock file")
        lock(LOCK_CREATE)

        logger.info("Created instance with success")
    }

    private fun copyFiles(resolvedFiles: List<File>) {
        GFileUtils.mkdirs(dir)
        val files = resolvedFiles.map {
            FileUtils.copyFileToDirectory(it, dir)
            File(dir, it.name)
        }
        findJar(files)?.let { FileUtils.moveFile(it, jar) }
    }

    private fun findJar(files: List<File>): File? {
        JAR_NAME_PATTERNS.forEach { pattern ->
            files.asSequence()
                    .filter { Patterns.wildcard(it.name, pattern) }
                    .forEach { return it }
        }

        return null
    }

    private fun validateFiles() {
        if (!jar.exists()) {
            throw SlingException("Instance JAR file not found at path: ${jar.absolutePath}. Is instance JAR URL configured?")
        }
    }

    private fun cleanDir(create: Boolean) {
        if (dir.exists()) {
            dir.deleteRecursively()
        }
        if (create) {
            dir.mkdirs()
        }
    }

    fun up() {
        if (!created) {
            logger.warn("Instance not created, so it could not be up: $this")
            return
        }


        logger.info("Executing start script: $startScript")
        execute(startScript)
    }

    fun down() {
        if (!created) {
            logger.warn("Instance not created, so it could not be down: $this")
            return
        }

        logger.info("Executing stop script: $stopScript")
        execute(stopScript)

        try {
            sync.stop()
        } catch (e: InstanceException) {
            // ignore, fallback when script failed
        }
    }

    fun init() {
        if (initialized) {
            logger.debug("Instance already initialized: $this")
            return
        }

        logger.info("Initializing running instance")
        config.upInitializer(this)
        lock(LOCK_INIT)
    }

    private fun execute(script: Script) {
        ProcessBuilder(*script.commandLine.toTypedArray())
                .directory(dir)
                .start()
    }

    val properties: Map<String, Any>
        get() {
            return mapOf(
                    "instance" to instance,
                    "instancePath" to dir.absolutePath,
                    "handle" to this
            )
        }

    fun destroy() {
        logger.info("Destroying at path '${dir.absolutePath}'")

        cleanDir(false)

        logger.info("Destroyed with success")
    }

    val sync by lazy {
        InstanceSync(project, instance)
    }

    val created: Boolean
        get() = locked(LOCK_CREATE)

    val initialized: Boolean
        get() = locked(LOCK_INIT)

    private fun lockFile(name: String): File = File(dir, "$name.lock")

    fun lock(name: String) {
        val metaJson = Formats.toJson(mapOf("locked" to Formats.date()))
        lockFile(name).printWriter().use { it.print(metaJson) }
    }

    fun locked(name: String): Boolean = lockFile(name).exists()

    override fun toString(): String {
        return "LocalHandle(dir=${dir.absolutePath}, instance=$instance)"
    }

}

val List<LocalHandle>.names: String?
    get() = if (isNotEmpty()) joinToString(", ") { it.instance.name } else "none"