package com.cognifide.gradle.sling.pkg.deploy

import com.cognifide.gradle.sling.api.SlingDefaultTask
import com.cognifide.gradle.sling.instance.Instance
import com.cognifide.gradle.sling.instance.InstanceSync
import com.cognifide.gradle.sling.instance.names
import com.cognifide.gradle.sling.instance.sync
import org.gradle.api.tasks.TaskAction

open class PurgeTask : SlingDefaultTask() {

    companion object {
        val NAME = "slingPurge"
    }

    init {
        description = "Uninstalls and then deletes CRX package on Sling instance(s)."
    }

    @TaskAction
    fun purge() {
        props.checkForce()

        val pkg = config.packageFileName
        val instances = Instance.filter(project)

        instances.sync(project, { sync ->
            try {
                val packagePath = sync.determineRemotePackagePath()

                uninstall(packagePath, sync)
                delete(packagePath, sync)
            } catch (e: DeployException) {
                logger.info(e.message)
                logger.debug("Nothing to purge.", e)
            }
        })

        notifier.default("Package purged", "$pkg from ${instances.names}")
    }

    private fun uninstall(packagePath: String, sync: InstanceSync) {
        try {
            sync.uninstallPackage(packagePath)
        } catch (e: DeployException) {
            logger.info("${e.message} Is it installed already?")
            logger.debug("Cannot uninstall package.", e)
        }
    }

    private fun delete(packagePath: String, sync: InstanceSync) {
        try {
            sync.deletePackage(packagePath)
        } catch (e: DeployException) {
            logger.info(e.message)
            logger.debug("Cannot delete package.", e)
        }
    }

}
