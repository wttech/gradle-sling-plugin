package com.cognifide.gradle.sling.instance

import com.cognifide.gradle.sling.api.SlingDefaultTask
import com.cognifide.gradle.sling.instance.action.AwaitAction
import org.gradle.api.tasks.TaskAction

open class UpTask : SlingDefaultTask() {

    companion object {
        val NAME = "slingUp"
    }

    init {
        description = "Turns on local Sling instance(s)."
    }

    @TaskAction
    fun up() {
        val handles = Instance.handles(project)

        handles.parallelStream().forEach { it.up() }
        AwaitAction(project, handles.map { it.instance }).perform()
        handles.parallelStream().forEach { it.init() }

        notifier.default("Instance(s) up", "Which: ${handles.names}")
    }

}