package com.cognifide.gradle.sling.instance

import com.cognifide.gradle.sling.api.SlingDefaultTask

open class SetupTask : SlingDefaultTask() {

    companion object {
        val NAME = "slingSetup"
    }

    init {
        description = "Creates and turns on local Sling instance(s) with satisfied dependencies and application built."
    }

}