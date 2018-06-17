package com.cognifide.gradle.sling.base.vlt

import com.cognifide.gradle.sling.api.SlingDefaultTask

open class SyncTask : SlingDefaultTask() {

    companion object {
        val NAME = "slingSync"
    }

    init {
        description = "Check out then clean JCR content."
    }
}