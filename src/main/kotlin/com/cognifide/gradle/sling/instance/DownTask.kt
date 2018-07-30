package com.cognifide.gradle.sling.instance

import com.cognifide.gradle.sling.api.SlingDefaultTask
import com.cognifide.gradle.sling.instance.action.ShutdownAction
import org.gradle.api.tasks.TaskAction

open class DownTask : SlingDefaultTask() {

    companion object {
        val NAME = "slingDown"
    }

    init {
        description = "Turns off local Sling instance(s)."
    }

    @TaskAction
    fun down() {
        val instances = Instance.filter(project)

        ShutdownAction(project, instances).perform()

        notifier.default("Instance(s) down", "Which: ${instances.names}")
    }

}