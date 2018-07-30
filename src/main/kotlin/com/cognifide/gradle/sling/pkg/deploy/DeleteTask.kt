package com.cognifide.gradle.sling.pkg.deploy

import com.cognifide.gradle.sling.api.SlingDefaultTask
import com.cognifide.gradle.sling.instance.Instance
import com.cognifide.gradle.sling.instance.names
import com.cognifide.gradle.sling.instance.sync
import org.gradle.api.tasks.TaskAction

open class DeleteTask : SlingDefaultTask() {

    companion object {
        val NAME = "slingDelete"
    }

    init {
        description = "Deletes Sling package on instance(s)."

        beforeExecuted { props.checkForce() }
    }

    @TaskAction
    fun delete() {
        val instances = Instance.filter(project)
        val pkg = config.packageFileName

        instances.sync(project, { it.deletePackage(it.determineRemotePackagePath()) })

        notifier.default("Package deleted", "$pkg on ${instances.names}")
    }

}
