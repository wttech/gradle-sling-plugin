package com.cognifide.gradle.sling.pkg.deploy

import com.cognifide.gradle.sling.api.SlingDefaultTask
import com.cognifide.gradle.sling.instance.Instance
import com.cognifide.gradle.sling.instance.names
import com.cognifide.gradle.sling.instance.sync
import org.gradle.api.tasks.TaskAction

open class UninstallTask : SlingDefaultTask() {

    companion object {
        val NAME = "slingUninstall"
    }

    init {
        description = "Uninstalls Sling package on instance(s)."
    }

    @TaskAction
    fun uninstall() {
        props.checkForce()

        val instances = Instance.filter(project)
        val pkg = config.packageFileName

        instances.sync(project, { it.uninstallPackage(it.determineRemotePackagePath()) })

        notifier.default("Package uninstalled", "$pkg from ${instances.names}")
    }

}
