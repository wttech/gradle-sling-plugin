package com.cognifide.gradle.sling.pkg.deploy

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper

@JsonIgnoreProperties(ignoreUnknown = true)
class PackageResponse private constructor() {

    lateinit var operation: String

    lateinit var status: String

    lateinit var path: String

    val success: Boolean
        get() = (operation == "upload" && status == "successful")
                || (operation == "installation" && status == "done")

    @JsonProperty("package")
    lateinit var pkg: Package

    companion object {
        fun fromJson(json: String): PackageResponse {
            return ObjectMapper().readValue(json, PackageResponse::class.java)
        }
    }
}