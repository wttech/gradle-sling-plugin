package com.cognifide.gradle.sling.common.instance.service.pkg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class DeleteResponse private constructor() {

    lateinit var operation: String

    lateinit var status: String

    lateinit var path: String

    val success: Boolean get() = (operation == "delete" && status == "successful")
}
