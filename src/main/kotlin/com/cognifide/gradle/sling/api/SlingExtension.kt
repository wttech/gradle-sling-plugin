package com.cognifide.gradle.sling.api

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

/**
 * Extension holding collection of Sling configuration properties.
 * Provides also nice DSL for configuring OSGi bundles and configuring custom interactive
 * build notifications.
 */
open class SlingExtension(@Transient private val project: Project) {

    companion object {
        val NAME = "sling"
    }

    val config = SlingConfig(project)

    val bundle = SlingBundle(project)

    val notifier = SlingNotifier.of(project)

    fun config(closure: Closure<*>) {
        ConfigureUtil.configure(closure, config)
    }

    fun bundle(closure: Closure<*>) {
        ConfigureUtil.configure(closure, bundle)
    }

    fun notifier(closure: Closure<*>) {
        ConfigureUtil.configure(closure, notifier)
    }

}