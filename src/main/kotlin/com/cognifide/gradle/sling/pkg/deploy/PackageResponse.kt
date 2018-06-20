package com.cognifide.gradle.sling.pkg.deploy

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper

@JsonIgnoreProperties(ignoreUnknown = true)
abstract class PackageResponse {

    lateinit var operation: String

    lateinit var status: String

    lateinit var path: String

    abstract val success: Boolean

}