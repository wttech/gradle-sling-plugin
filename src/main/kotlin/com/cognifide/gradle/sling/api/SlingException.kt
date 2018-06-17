package com.cognifide.gradle.sling.api

import org.gradle.api.GradleException

open class SlingException : GradleException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)

}
