package com.cognifide.gradle.sling.instance

import com.cognifide.gradle.sling.api.SlingDefaultTask
import com.cognifide.gradle.sling.instance.action.AwaitAction
import org.gradle.api.tasks.TaskAction

open class AwaitTask : SlingDefaultTask() {

    companion object {
        const val NAME = "slingAwait"
    }

    init {
        description = "Waits until all local Sling instance(s) be stable."
    }

    @TaskAction
    fun await() {
        val instances = Instance.filter(project)

        AwaitAction(project, instances).perform()
    }

}