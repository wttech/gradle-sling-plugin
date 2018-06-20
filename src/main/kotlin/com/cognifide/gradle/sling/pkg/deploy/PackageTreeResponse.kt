package com.cognifide.gradle.sling.pkg.deploy

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper

@JsonIgnoreProperties(ignoreUnknown = true)
class PackageTreeResponse private constructor() {

    @JsonProperty("children")
    lateinit var packages: List<Package>

    companion object {
        fun fromJson(json: String): PackageTreeResponse {
            return ObjectMapper().readValue(json, PackageTreeResponse::class.java)
        }
    }

}