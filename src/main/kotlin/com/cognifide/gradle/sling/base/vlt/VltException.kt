package com.cognifide.gradle.sling.base.vlt

import com.cognifide.gradle.sling.api.SlingException

class VltException : SlingException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)

}
