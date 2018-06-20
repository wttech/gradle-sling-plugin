package com.cognifide.gradle.sling.pkg.deploy

import com.fasterxml.jackson.databind.ObjectMapper

class ListResponse private constructor() {

    companion object {
        fun fromJson(json: String): List<Package> {
            return ObjectMapper().readValue(json, Array<Package>::class.java).toList()
        }
    }

}