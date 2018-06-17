package com.cognifide.gradle.sling.bundle

import com.cognifide.gradle.sling.api.SlingException

class BundleException : SlingException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)

}
