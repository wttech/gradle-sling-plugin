package com.cognifide.gradle.sling.pkg.deploy

import com.cognifide.gradle.sling.api.SlingDefaultTask
import com.cognifide.gradle.sling.instance.Instance
import com.cognifide.gradle.sling.instance.names
import com.cognifide.gradle.sling.instance.sync
import org.gradle.api.tasks.TaskAction

open class DeployTask : SlingDefaultTask() {

    companion object {
        val NAME = "slingDeploy"
    }

    init {
        description = "Deploys Vault package on instance(s). Upload then install (and optionally activate)."
    }

    @TaskAction
    fun deploy() {
        val pkg = config.packageFile
        val instances = Instance.filter(project)

        instances.sync(project, { it.deployPackage(pkg) })

        notifier.default("Package deployed", "${pkg.name} on ${instances.names}")
    }

}