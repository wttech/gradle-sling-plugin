package com.cognifide.gradle.sling.pkg.deploy

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper

@JsonIgnoreProperties(ignoreUnknown = true)
class UploadResponse : PackageResponse() {

    override val success: Boolean
        get() =  (operation == "upload" && status == "done")

    @JsonProperty("package")
    lateinit var pkg: Package

    companion object {
        fun fromJson(json: String): UploadResponse {
            return ObjectMapper().readValue(json, UploadResponse::class.java)
        }
    }
}