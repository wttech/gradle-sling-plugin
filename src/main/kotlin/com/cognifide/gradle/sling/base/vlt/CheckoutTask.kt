package com.cognifide.gradle.sling.base.vlt

import com.cognifide.gradle.sling.api.SlingDefaultTask
import com.cognifide.gradle.sling.internal.Formats
import org.gradle.api.tasks.TaskAction

// TODO https://github.com/Cognifide/gradle-sling-plugin/issues/135
open class CheckoutTask : SlingDefaultTask() {

    companion object {
        val NAME = "slingCheckout"
    }

    init {
        description = "Check out JCR content from running Sling instance."
    }

    @TaskAction
    fun checkout() {
        val runner = VltRunner(project)
        runner.checkout()
        notifier.default("Checked out JCR content", "Instance: ${runner.checkoutInstance.name}. Directory: ${Formats.rootProjectPath(config.contentPath, project)}")
    }

}