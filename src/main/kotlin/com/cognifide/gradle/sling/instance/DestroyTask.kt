package com.cognifide.gradle.sling.instance

import com.cognifide.gradle.sling.api.SlingDefaultTask
import org.gradle.api.tasks.TaskAction

open class DestroyTask : SlingDefaultTask() {

    companion object {
        const val NAME = "slingDestroy"
    }

    init {
        description = "Destroys local Sling instance(s)."
    }

    @TaskAction
    fun destroy() {
        props.checkForce()

        val handles = Instance.handles(project)
        handles.parallelStream().forEach { it.destroy() }

        notifier.default("Instance(s) destroyed", "Which: ${handles.names}")
    }

}