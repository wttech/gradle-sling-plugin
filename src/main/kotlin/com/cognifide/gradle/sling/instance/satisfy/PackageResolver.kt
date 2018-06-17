package com.cognifide.gradle.sling.instance.satisfy

import com.cognifide.gradle.sling.internal.file.resolver.FileGroup
import com.cognifide.gradle.sling.internal.file.resolver.FileResolver
import org.gradle.api.Project
import java.io.File

class PackageResolver(project: Project, downloadDir: File) : FileResolver(project, downloadDir) {

    override fun createGroup(name: String): FileGroup {
        return PackageGroup(this, name)
    }
}