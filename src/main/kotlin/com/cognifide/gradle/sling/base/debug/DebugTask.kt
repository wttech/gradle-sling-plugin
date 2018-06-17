package com.cognifide.gradle.sling.base.debug

import com.cognifide.gradle.sling.api.SlingDefaultTask
import com.cognifide.gradle.sling.api.SlingTask
import com.cognifide.gradle.sling.internal.Formats
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

open class DebugTask : SlingDefaultTask() {

    companion object {
        val NAME = "slingDebug"
    }

    @OutputFile
    val file = SlingTask.temporaryFile(project, NAME, "debug.json")

    init {
        description = "Dumps effective Sling build configuration of project to JSON file"

        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun debug() {
        logger.lifecycle("Dumping Sling build configuration of $project to file: $file")

        val props = ProjectDumper(project).properties
        val json = Formats.toJson(props)

        file.bufferedWriter().use { it.write(json) }
        logger.info(json)

        notifier.default("Configuration dumped", "For $project to file: ${Formats.projectPath(file, project)}")
    }

}
