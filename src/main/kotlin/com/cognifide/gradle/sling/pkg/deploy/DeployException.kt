package com.cognifide.gradle.sling.pkg.deploy

import com.cognifide.gradle.sling.api.SlingException

class DeployException : SlingException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)

}
