package com.cognifide.gradle.sling.instance

import com.cognifide.gradle.sling.api.SlingDefaultTask
import com.cognifide.gradle.sling.api.SlingTask
import com.cognifide.gradle.sling.pkg.deploy.Package
import com.cognifide.gradle.sling.instance.satisfy.PackageGroup
import com.cognifide.gradle.sling.instance.satisfy.PackageResolver
import com.cognifide.gradle.sling.internal.Patterns
import groovy.lang.Closure
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.util.ConfigureUtil
import java.io.File

open class SatisfyTask : SlingDefaultTask() {

    @get:Internal
    val packageProvider = PackageResolver(project, SlingTask.temporaryDir(project, NAME, DOWNLOAD_DIR))

    @get:Internal
    var groupFilter: (String) -> Boolean = { fileGroup ->
        Patterns.wildcards(fileGroup, config.satisfyGroupName)
    }

    @get:Internal
    val outputDirs: List<File>
        get() = packageProvider.outputDirs(groupFilter)

    @get:Internal
    val allFiles: List<File>
        get() = packageProvider.allFiles(groupFilter)

    @get:Internal
    val packageGroups by lazy {
        val result = if (cmdGroups) {
            logger.info("Providing packages defined via command line.")
            packageProvider.filterGroups("cmd.*")
        } else {
            logger.info("Providing packages defined in build script.")
            packageProvider.filterGroups(groupFilter)
        }

        val files = result.flatMap { it.files }

        logger.info("Packages provided (${files.size}).")

        @Suppress("unchecked_cast")
        result as List<PackageGroup>
    }

    @get:Internal
    val cmdGroups: Boolean
        get() = project.properties["sling.satisfy.urls"] != null

    init {
        group = SlingTask.GROUP
        description = "Satisfies Sling by uploading & installing dependent packages on instance(s)."

        defineCmdGroups()
    }

    fun defineCmdGroups() {
        if (cmdGroups) {
            props.list("sling.satisfy.urls").forEachIndexed { index, url ->
                packageProvider.group("cmd.${index + 1}", { url(url) })
            }
        }
    }

    fun packages(closure: Closure<*>) {
        ConfigureUtil.configure(closure, packageProvider)
    }

    @TaskAction
    fun satisfy() {
        val actions = mutableListOf<Action>()

        for (packageGroup in packageGroups) {
            logger.info("Satisfying group of packages '${packageGroup.name}'.")

            var anyPackageSatisfied = false

            packageGroup.instances.sync(project, { sync ->
                val packageStates = packageGroup.files.fold(mutableMapOf<File, Package?>(), { states, pkg ->
                    states[pkg] = sync.determineRemotePackage(pkg, config.satisfyRefreshing); states
                })
                val anyPackageSatisfiable = packageStates.any {
                    sync.isSnapshot(it.key) || it.value == null || !it.value!!.installed
                }

                if (anyPackageSatisfiable) {
                    packageGroup.initializer(sync)
                }

                packageStates.forEach { (pkg, state) ->
                    when {
                        sync.isSnapshot(pkg) -> {
                            logger.lifecycle("Satisfying package ${pkg.name} on ${sync.instance.name} (snapshot).")
                            sync.deployPackage(pkg)

                            anyPackageSatisfied = true
                            actions.add(Action(pkg, sync.instance))
                        }
                        state == null -> {
                            logger.lifecycle("Satisfying package ${pkg.name} on ${sync.instance.name} (not uploaded).")
                            sync.deployPackage(pkg)

                            anyPackageSatisfied = true
                            actions.add(Action(pkg, sync.instance))
                        }
                        !state.installed -> {
                            logger.lifecycle("Satisfying package ${pkg.name} on ${sync.instance.name} (not installed).")
                            sync.installPackage(state.path)

                            anyPackageSatisfied = true
                            actions.add(Action(pkg, sync.instance))
                        }
                        else -> {
                            logger.lifecycle("Not satisfying package: ${pkg.name} on ${sync.instance.name} (already installed).")
                        }
                    }
                }

                if (anyPackageSatisfiable) {
                    packageGroup.finalizer(sync)
                }
            })

            if (anyPackageSatisfied) {
                packageGroup.completer()
            }
        }

        if (actions.isNotEmpty()) {
            val packages = actions.map { it.pkg }.toSet()
            val instances = actions.map { it.instance }.toSet()

            if (packages.size == 1) {
                notifier.default("Package satisfied", "${packages.first().name} on ${instances.names}")
            } else {
                notifier.default("Packages satisfied", "Performed ${actions.size} action(s) for ${packages.size} package(s) on ${instances.size} instance(s).")
            }
        }
    }

    class Action(val pkg: File, val instance: Instance)

    companion object {
        const val NAME = "slingSatisfy"

        const val DOWNLOAD_DIR = "download"
    }

}