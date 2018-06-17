package com.cognifide.gradle.sling.api

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.Serializable

class SlingPlugin private constructor() {

    class Build : Serializable {

        lateinit var pluginVersion: String

        lateinit var gradleVersion: String

    }

    companion object {

        val BUILD by lazy {
            fromJson(SlingPlugin::class.java.getResourceAsStream("/build.json")
                    .bufferedReader().use { it.readText() })
        }

        val NAME = "Gradle Sling Plugin"

        val NAME_WITH_VERSION: String
            get() = "$NAME ${BUILD.pluginVersion}"

        private fun fromJson(json: String): Build {
            return ObjectMapper().readValue(json, Build::class.java)
        }

        private var once = false

        @Synchronized
        fun once(callback: (Build) -> Unit) {
            if (!once) {
                callback(BUILD)
                once = true
            }
        }

    }

}