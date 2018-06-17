package com.cognifide.gradle.sling.api

import org.gradle.api.Project
import org.gradle.util.GFileUtils
import java.io.File

interface SlingTask {

    companion object {
        val GROUP = "Sling"

        fun temporaryDir(project: Project, taskName: String, path: String): File {
            val dir = File(project.buildDir, "sling/$taskName/$path")

            GFileUtils.mkdirs(dir)

            return dir
        }

        fun temporaryFile(project: Project, taskName: String, name: String): File {
            val dir = File(project.buildDir, "sling/$taskName")

            GFileUtils.mkdirs(dir)

            return File(dir, name)
        }
    }

    val config: SlingConfig

}