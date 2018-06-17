package com.cognifide.gradle.sling.instance.satisfy

import com.cognifide.gradle.sling.api.SlingException

class PackageException : SlingException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)

}