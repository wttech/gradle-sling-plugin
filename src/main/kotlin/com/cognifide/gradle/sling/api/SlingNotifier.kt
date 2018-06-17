package com.cognifide.gradle.sling.api

import dorkbox.notify.Notify
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.gradle.api.Project
import javax.imageio.ImageIO

class SlingNotifier private constructor(private val project: Project) {

    private val config by lazy { SlingConfig.of(project) }

    fun log(title: String, message: String) {
        project.logger.lifecycle(if (message.isNotBlank()) {
            "${title.removeSuffix(".")}. $message"
        } else {
            title
        })
    }

    fun default(title: String, message: String = "") {
        if (config.notificationEnabled) {
            now(title, message)
        }

        log(title, message)
    }

    // TODO allow to customize color of plugin logo (warn, info, error etc)
    fun now(title: String, text: String = "") {
        now {
            title(title)
            text(StringUtils.replace(text, "\n", "<br>"))
        }
    }

    fun now(configurer: Notify.() -> Unit) {
        try {
            Notify.create()
                    .image(ImageIO.read(javaClass.getResourceAsStream(IMAGE_PATH)))
                    .apply(config.notificationConfig)
                    .apply(configurer)
                    .show()
        } catch (e: Exception) {
            project.logger.debug("Cannot show system notification", e)
        }
    }

    companion object {

        const val IMAGE_PATH = "/com/cognifide/gradle/sling/META-INF/vault/definition/thumbnail.png"

        const val EXT_INSTANCE_PROP = "slingNotifier"

        /**
         * Get project specific notifier (config can vary)
         */
        fun of(project: Project): SlingNotifier {
            val props = project.extensions.extraProperties
            if (!props.has(EXT_INSTANCE_PROP)) {
                props.set(EXT_INSTANCE_PROP, setup(project))
            }

            return props.get(EXT_INSTANCE_PROP) as SlingNotifier
        }

        /**
         *
         */
        private fun setup(project: Project): SlingNotifier {
            val notifier = SlingNotifier(project)

            if (project == project.rootProject) {
                project.gradle.buildFinished {
                    if (it.failure != null) {
                        val exception = ExceptionUtils.getRootCause(it.failure)
                        val message = exception?.message ?: "no error message"

                        notifier.default("Build failure", message)
                    }
                }
            }

            return notifier
        }

    }

}