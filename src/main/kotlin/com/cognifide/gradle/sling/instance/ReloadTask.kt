package com.cognifide.gradle.sling.instance

import com.cognifide.gradle.sling.api.SlingDefaultTask
import com.cognifide.gradle.sling.instance.action.ReloadAction
import org.gradle.api.tasks.TaskAction

open class ReloadTask : SlingDefaultTask() {

    companion object {
        val NAME = "slingReload"
    }

    init {
        description = "Reloads all Sling instance(s)."
    }

    @TaskAction
    fun reload() {
        val instances = Instance.filter(project)

        ReloadAction(project, instances).perform()
        notifier.default("Instance(s) reloaded", "Which: ${instances.names}")
    }

}