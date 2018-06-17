package com.cognifide.gradle.sling.pkg.deploy

import com.cognifide.gradle.sling.api.SlingDefaultTask
import com.cognifide.gradle.sling.instance.Instance
import com.cognifide.gradle.sling.instance.names
import com.cognifide.gradle.sling.instance.sync
import org.gradle.api.tasks.TaskAction

open class ActivateTask : SlingDefaultTask() {

    companion object {
        val NAME = "slingActivate"
    }

    init {
        description = "Activates CRX package on instance(s)."
    }

    @TaskAction
    fun activate() {
        val pkg = config.packageFileName
        val instances = Instance.filter(project)

        instances.sync(project, { it.activatePackage(it.determineRemotePackagePath()) })

        notifier.default("Package activated", "$pkg on ${instances.names}")
    }

}
