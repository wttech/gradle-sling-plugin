package com.cognifide.gradle.sling.instance

import com.cognifide.gradle.sling.api.SlingDefaultTask

open class RestartTask : SlingDefaultTask() {

    companion object {
        val NAME = "slingRestart"
    }

    init {
        description = "Turns off then on local Sling instance(s)."
    }

}