package com.cognifide.gradle.sling.base.vlt

import com.cognifide.gradle.sling.api.SlingDefaultTask
import org.gradle.api.tasks.TaskAction

open class VltTask : SlingDefaultTask() {

    companion object {
        const val NAME = "slingVlt"
    }

    init {
        description = "Execute any Vault command."
    }

    @TaskAction
    fun perform() {
        val command = project.properties["sling.vlt.command"] as String?
        if (command.isNullOrBlank()) {
            throw VltException("Vault command cannot be blank.")
        }

        VltRunner(project).raw(command!!)
        notifier.default("Executing Vault command", "Command '$command' finished.")
    }

}