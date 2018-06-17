package com.cognifide.gradle.sling.pkg.deploy

import com.cognifide.gradle.sling.api.SlingDefaultTask
import com.cognifide.gradle.sling.instance.Instance
import com.cognifide.gradle.sling.instance.names
import com.cognifide.gradle.sling.instance.sync
import org.gradle.api.tasks.TaskAction

open class UploadTask : SlingDefaultTask() {

    companion object {
        val NAME = "slingUpload"
    }

    init {
        description = "Uploads CRX package to instance(s)."
    }

    @TaskAction
    fun upload() {
        val pkg = config.packageFile
        val instances = Instance.filter(project)

        instances.sync(project, { it.uploadPackage(pkg) })

        notifier.default("Package uploaded", "${pkg.name} on ${instances.names}")
    }

}
