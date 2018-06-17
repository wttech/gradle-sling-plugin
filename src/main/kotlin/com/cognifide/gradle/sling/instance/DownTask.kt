package com.cognifide.gradle.sling.instance

import com.cognifide.gradle.sling.api.SlingDefaultTask
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
        val handles = Instance.handles(project)
        handles.parallelStream().forEach { it.down() }

        notifier.default("Instance(s) down", "Which: ${handles.names}")
    }

}