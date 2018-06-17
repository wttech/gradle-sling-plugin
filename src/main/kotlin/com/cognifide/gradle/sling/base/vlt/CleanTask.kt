package com.cognifide.gradle.sling.base.vlt

import com.cognifide.gradle.sling.api.SlingDefaultTask
import com.cognifide.gradle.sling.internal.Formats
import org.gradle.api.tasks.TaskAction

open class CleanTask : SlingDefaultTask() {

    companion object {
        val NAME = "slingClean"
    }

    init {
        description = "Clean checked out JCR content."
    }

    @TaskAction
    fun clean() {
        VltRunner(project).clean()
        notifier.default("Cleaned JCR content", "Directory: ${Formats.rootProjectPath(config.contentPath, project)}")
    }

}