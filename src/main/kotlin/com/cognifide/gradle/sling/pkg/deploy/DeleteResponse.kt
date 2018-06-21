package com.cognifide.gradle.sling.pkg.deploy

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper

@JsonIgnoreProperties(ignoreUnknown = true)
class DeleteResponse private constructor() {

    lateinit var operation: String

    lateinit var status: String

    lateinit var path: String

    val success: Boolean
        get() = (operation == "delete" && status == "successful")

    companion object {
        fun fromJson(json: String): DeleteResponse {
            return ObjectMapper().readValue(json, DeleteResponse::class.java)
        }
    }
}