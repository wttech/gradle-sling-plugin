package com.cognifide.gradle.sling.api

import com.cognifide.gradle.sling.internal.PropertyParser
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested

abstract class SlingDefaultTask : DefaultTask(), SlingTask {

    @Nested
    final override val config = SlingConfig.of(project)

    @Internal
    protected val notifier = SlingNotifier.of(project)

    @Internal
    protected val props = PropertyParser(project)

    init {
        group = SlingTask.GROUP
    }

}