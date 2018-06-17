package com.cognifide.gradle.sling.base.vlt

import com.cognifide.gradle.sling.api.SlingDefaultTask
import org.gradle.api.tasks.TaskAction

open class RcpTask : SlingDefaultTask() {

    companion object {
        val NAME = "slingRcp"
    }

    init {
        description = "Copy JCR content from instance to another."
    }

    @TaskAction
    fun rcp() {
        val runner = VltRunner(project)
        runner.rcp()

        if (!runner.rcpSourceInstance.cmd && !runner.rcpTargetInstance.cmd) {
            notifier.default("RCP finished", "Copied ${runner.rcpPaths.size} JCR root(s) from instance ${runner.rcpSourceInstance.name} to ${runner.rcpTargetInstance.name}.")
        } else {
            notifier.default("RCP finished", "Copied ${runner.rcpPaths.size} JCR root(s) between instances.")
        }
    }

}