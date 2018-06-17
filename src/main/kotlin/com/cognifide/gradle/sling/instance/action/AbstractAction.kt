package com.cognifide.gradle.sling.instance.action

import com.cognifide.gradle.sling.api.SlingConfig
import com.cognifide.gradle.sling.api.SlingNotifier
import com.cognifide.gradle.sling.instance.InstanceAction
import org.gradle.api.Project

abstract class AbstractAction(val project: Project) : InstanceAction {

    val config = SlingConfig.of(project)

    val notifier = SlingNotifier.of(project)

    val logger = project.logger

    var notify = true

    fun notify(title: String, text: String) {
        if (notify) {
            notifier.default(title, text)
        } else {
            notifier.log(title, text)
        }
    }

}