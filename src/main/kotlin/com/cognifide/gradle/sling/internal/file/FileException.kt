package com.cognifide.gradle.sling.internal.file

import com.cognifide.gradle.sling.api.SlingException

class FileException : SlingException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)

}
