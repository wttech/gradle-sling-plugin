package com.cognifide.gradle.sling.instance

import com.cognifide.gradle.sling.api.SlingDefaultTask
import org.gradle.api.tasks.TaskAction

open class SetupTask : SlingDefaultTask() {

    companion object {
        val NAME = "slingSetup"
    }

    init {
        description = "Creates and turns on local Sling instance(s) with satisfied dependencies and application built."
    }

    @TaskAction
    fun setup() {
        notifier.default("Instance(s) setup", "Finished with success.")
    }

}