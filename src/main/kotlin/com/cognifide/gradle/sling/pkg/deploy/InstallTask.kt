package com.cognifide.gradle.sling.pkg.deploy

import com.cognifide.gradle.sling.api.SlingDefaultTask
import com.cognifide.gradle.sling.instance.Instance
import com.cognifide.gradle.sling.instance.names
import com.cognifide.gradle.sling.instance.sync
import org.gradle.api.tasks.TaskAction

open class InstallTask : SlingDefaultTask() {

    companion object {
        val NAME = "slingInstall"
    }

    init {
        description = "Installs CRX package on instance(s)."
    }

    @TaskAction
    fun install() {
        val instances = Instance.filter(project)
        val pkg = config.packageFileName

        instances.sync(project, { it.installPackage(it.determineRemotePackagePath()) })

        notifier.default("Package installed", "$pkg on ${instances.names}")
    }

}
