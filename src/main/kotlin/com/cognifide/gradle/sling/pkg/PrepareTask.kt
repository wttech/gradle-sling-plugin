package com.cognifide.gradle.sling.pkg

import com.cognifide.gradle.sling.api.SlingDefaultTask
import com.cognifide.gradle.sling.api.SlingTask
import com.cognifide.gradle.sling.internal.file.FileOperations
import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

open class PrepareTask : SlingDefaultTask() {

    companion object {
        val NAME = "slingPrepare"
    }

    @OutputDirectory
    val vaultDir = SlingTask.temporaryDir(project, NAME, PackagePlugin.VLT_PATH)

    init {
        description = "Prepare Vault files before composing Vault package"

        project.afterEvaluate {
            config.vaultFilesDirs.forEach { dir -> inputs.dir(dir) }
        }
    }

    @TaskAction
    fun prepare() {
        copyContentVaultFiles()
        copyMissingVaultFiles()
    }

    private fun copyContentVaultFiles() {
        if (vaultDir.exists()) {
            vaultDir.deleteRecursively()
        }
        vaultDir.mkdirs()

        val dirs = config.vaultFilesDirs

        if (dirs.isEmpty()) {
            logger.info("None of Vault files directories exist: $dirs. Only generated defaults will be used.")
        } else {
            dirs.onEach { dir ->
                logger.info("Copying Vault files from path: '${dir.absolutePath}'")

                FileUtils.copyDirectory(dir, vaultDir)
            }
        }
    }

    private fun copyMissingVaultFiles() {
        if (!config.vaultCopyMissingFiles) {
            return
        }

        FileOperations.copyResources(PackagePlugin.VLT_PATH, vaultDir, true)
    }
}