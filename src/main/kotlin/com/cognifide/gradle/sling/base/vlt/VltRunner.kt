package com.cognifide.gradle.sling.base.vlt

import com.cognifide.gradle.sling.api.SlingConfig
import com.cognifide.gradle.sling.instance.Instance
import com.cognifide.gradle.sling.internal.PropertyParser
import com.cognifide.gradle.sling.internal.file.FileOperations
import com.cognifide.gradle.sling.pkg.PackagePlugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File

class VltRunner(val project: Project) {

    val logger: Logger = project.logger

    val props = PropertyParser(project)

    val config = SlingConfig.of(project)

    val workingDir: File
        get() {
            var path = "${config.contentPath}/${PackagePlugin.JCR_ROOT}"

            val relativePath = project.properties["sling.vlt.path"] as String?
            if (!relativePath.isNullOrBlank()) {
                path = "$path/$relativePath"
            }

            return File(path)
        }

    fun raw(command: String, props: Map<String, Any> = mapOf()) {
        val app = VltApp(project)

        val allProps = mapOf("instances" to config.instances) + props
        val fullCommand = this.props.expand(command, allProps)

        logger.lifecycle("Working directory: $workingDir")
        logger.lifecycle("Executing command: vlt $command")

        app.execute(fullCommand, workingDir.absolutePath)
    }

    fun checkout() {
        val contentDir = File(config.contentPath)
        if (!contentDir.exists()) {
            logger.info("JCR content directory to be checked out does not exist: ${contentDir.absolutePath}")
        }

        checkoutFilter.use {
            raw("--credentials ${checkoutInstance.credentials} checkout --force --filter ${checkoutFilter.file.absolutePath} ${checkoutInstance.httpUrl}/crx/server/crx.default")
        }
    }

    val checkoutFilter by lazy { determineCheckoutFilter() }

    private fun determineCheckoutFilter(): VltFilter {
        val cmdFilterRoots = props.list("sling.checkout.filterRoots")

        return if (cmdFilterRoots.isNotEmpty()) {
            logger.info("Using Vault filter roots specified as command line property: $cmdFilterRoots")
            VltFilter.temporary(project, cmdFilterRoots)
        } else {
            if (config.checkoutFilterPath.isNotBlank()) {
                val configFilter = FileOperations.find(project, config.vaultPath, config.checkoutFilterPath)
                        ?: throw VltException("Vault check out filter file does not exist at path: ${config.checkoutFilterPath} (or under directory: ${config.vaultPath}).")
                VltFilter(configFilter)
            } else {
                val conventionFilter = FileOperations.find(project, config.vaultPath, config.checkoutFilterPaths)
                        ?: throw VltException("None of Vault check out filter file does not exist at one of convention paths: ${config.checkoutFilterPaths}.")
                VltFilter(conventionFilter)
            }
        }
    }

    val checkoutInstance: Instance by lazy { determineCheckoutInstance() }

    private fun determineCheckoutInstance(): Instance {
        val cmdInstanceArg = props.string("sling.checkout.instance")
        if (!cmdInstanceArg.isNullOrBlank()) {
            val cmdInstance = config.parseInstance(cmdInstanceArg!!)

            logger.info("Using instance specified by command line parameter: $cmdInstance")
            return cmdInstance
        }

        val namedInstance = Instance.filter(project, config.instanceName).firstOrNull()
        if (namedInstance != null) {
            logger.info("Using first instance matching filter '${config.instanceName}': $namedInstance")
            return namedInstance
        }

        val anyInstance = Instance.filter(project, Instance.FILTER_ANY).firstOrNull()
        if (anyInstance != null) {
            logger.info("Using first instance matching filter '${Instance.FILTER_ANY}': $anyInstance")
            return anyInstance
        }

        throw VltException("Vault instance cannot be determined neither by command line parameter nor Sling config.")
    }

    fun clean() {
        val contentDir = File(config.contentPath)
        if (!contentDir.exists()) {
            logger.warn("JCR content directory to be cleaned does not exist: ${contentDir.absolutePath}")
            return
        }

        logger.info("Cleaning using $checkoutFilter")

        val roots = checkoutFilter.rootPaths
                .map { File(contentDir, "${PackagePlugin.JCR_ROOT}/${it.removeSurrounding("/")}") }
                .filter { it.exists() }
        if (roots.isEmpty()) {
            logger.warn("For given filter there is no existing roots to be cleaned.")
            return
        }

        roots.forEach { root ->
            logger.lifecycle("Cleaning root: $root")
            VltCleaner(project, root).clean()
        }
    }

    fun rcp() {
        rcpPaths.forEach { sourcePath, targetPath ->
            raw("rcp $rcpOpts ${rcpSourceInstance.httpBasicAuthUrl}/crx/-/jcr:root$sourcePath ${rcpTargetInstance.httpBasicAuthUrl}/crx/-/jcr:root$targetPath")
        }
    }

    val rcpPaths: Map<String, String> by lazy {
        val paths = props.list("sling.rcp.paths")
        if (paths.isEmpty()) {
            throw VltException("RCP param '-Psling.rcp.paths' is not specified.")
        }

        paths.fold(mutableMapOf<String, String>(), { r, path ->
            val parts = path.split("=").map { it.trim() }
            when (parts.size) {
                1 -> r[path] = path
                2 -> r[parts[0]] = parts[1]
                else -> throw VltException("RCP path has invalid format: $path")
            }
            r
        })
    }

    val rcpOpts: String by lazy { project.properties["sling.rcp.opts"] as String? ?: "-b 100 -r -u" }

    val rcpSourceInstance: Instance by lazy {
        config.parseInstance(project.properties["sling.rcp.source.instance"] as String?
                ?: throw VltException("RCP param '-Psling.rcp.source.instance' is not specified."))
    }

    val rcpTargetInstance: Instance by lazy {
        config.parseInstance(project.properties["sling.rcp.target.instance"] as String?
                ?: throw VltException("RCP param '-Psling.rcp.target.instance' is not specified."))
    }

}