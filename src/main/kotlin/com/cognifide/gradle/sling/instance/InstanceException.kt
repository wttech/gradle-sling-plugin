package com.cognifide.gradle.sling.instance

import com.cognifide.gradle.sling.api.SlingException

class InstanceException : SlingException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)

}
