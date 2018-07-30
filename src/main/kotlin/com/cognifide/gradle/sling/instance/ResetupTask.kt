package com.cognifide.gradle.sling.instance

import com.cognifide.gradle.sling.api.SlingDefaultTask

open class ResetupTask : SlingDefaultTask() {

    companion object {
        val NAME = "slingResetup"
    }

    init {
        description = "Destroys then sets up local Sling instance(s)."
    }

}