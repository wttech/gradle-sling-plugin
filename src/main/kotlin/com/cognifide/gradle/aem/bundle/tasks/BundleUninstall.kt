package com.cognifide.gradle.aem.bundle.tasks

import com.cognifide.gradle.aem.common.instance.checkAvailable
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.BundleTask
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class BundleUninstall : BundleTask() {

    @TaskAction
    fun uninstall() {
        instances.get().checkAvailable()
        sync { osgiFramework.uninstallBundle(it) }
        common.notifier.notify("Bundle uninstalled", "${bundles.get().fileNames} on ${instances.get().names}")
    }

    init {
        description = "Uninstalls OSGi bundle on instance(s)."
    }

    companion object {
        const val NAME = "bundleUninstall"
    }
}
