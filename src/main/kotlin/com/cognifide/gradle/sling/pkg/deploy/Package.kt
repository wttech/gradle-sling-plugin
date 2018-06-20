package com.cognifide.gradle.sling.pkg.deploy

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class Package private constructor() {

    lateinit var definition: Definition

    lateinit var path: String

    val installed: Boolean
        get() = definition.lastUnpacked != null

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Definition {

        lateinit var group: String

        lateinit var name: String

        lateinit var version: String

        @JsonProperty("jcr:description")
        var description: String? = null

        @JsonProperty("jcr:lastModified")
        var lastModified: String? = null

        var lastUnpacked: String? = null

    }

}