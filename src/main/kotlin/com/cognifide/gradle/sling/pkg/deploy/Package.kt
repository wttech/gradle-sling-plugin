package com.cognifide.gradle.sling.pkg.deploy

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class Package private constructor() {

    lateinit var definition: Definition

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Definition {

        lateinit var group: String

        lateinit var name: String

        lateinit var version: String

        @JsonProperty("jcr:lastModified")
        lateinit var modified: String

    }

}