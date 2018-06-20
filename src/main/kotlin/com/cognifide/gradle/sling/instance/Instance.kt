package com.cognifide.gradle.sling.instance

import com.cognifide.gradle.sling.api.SlingConfig
import com.cognifide.gradle.sling.api.SlingException
import com.cognifide.gradle.sling.internal.Formats
import com.cognifide.gradle.sling.internal.Patterns
import com.fasterxml.jackson.annotation.JsonIgnore
import org.gradle.api.Project
import java.io.Serializable
import kotlin.reflect.KClass

interface Instance : Serializable {

    companion object {

        val FILTER_ANY = "*"

        val ENVIRONMENT_CMD = "cmd"

        val URL_DEFAULT = "http://localhost:8080"

        val USER_DEFAULT = "admin"

        val PASSWORD_DEFAULT = "admin"

        val TYPE_DEFAULT = "main"

        fun parse(str: String): List<RemoteInstance> {
            return str.split(";").map { urlRaw ->
                val parts = urlRaw.split(",")

                when (parts.size) {
                    4 -> {
                        val (httpUrl, type, user, password) = parts

                        RemoteInstance.create(httpUrl, {
                            this.user = user
                            this.password = password
                            this.type = type
                        })
                    }
                    3 -> {
                        val (httpUrl, user, password) = parts

                        RemoteInstance.create(httpUrl, {
                            this.user = user
                            this.password = password
                        })
                    }
                    else -> {
                        RemoteInstance.create(urlRaw)
                    }
                }
            }
        }

        fun properties(project: Project): List<Instance> {
            val localInstances = collectProperties(project, "local").map {
                val (name, props) = it
                val nameParts = name.split("-")
                if (nameParts.size != 2) {
                    throw InstanceException("Local instance name has invalid format: '$name'.")
                }
                val (environment, typeName) = nameParts
                val httpUrl = props["httpUrl"]
                        ?: throw InstanceException("Local instance named '$name' must have property 'httpUrl' defined.")

                LocalInstance.create(httpUrl, {
                    this.environment = environment
                    this.type = typeName

                    props["password"]?.let { this.password = it }
                    props["jvmOpts"]?.let { this.jvmOpts = it.split(" ") }
                    props["startOpts"]?.let { this.startOpts = it.split(" ") }
                    props["runModes"]?.let { this.runModes = it.split(",") }
                    props["debugPort"]?.let { this.debugPort = it.toInt() }
                })
            }.sortedBy { it.name }

            val remoteInstances = collectProperties(project, "remote").map {
                val (name, props) = it
                val nameParts = name.split("-")
                if (nameParts.size != 2) {
                    throw InstanceException("Remote instance name has invalid format: '$name'.")
                }
                val (environment, typeName) = nameParts
                val httpUrl = props["httpUrl"]
                        ?: throw InstanceException("Remote instance named '$name' must have property 'httpUrl' defined.")

                RemoteInstance.create(httpUrl, {
                    this.environment = environment
                    this.type = typeName

                    props["user"]?.let { this.user = it }
                    props["password"]?.let { this.password = it }

                })
            }.sortedBy { it.name }

            return localInstances + remoteInstances
        }

        private fun collectProperties(project: Project, type: String): MutableMap<String, MutableMap<String, String>> {
            return project.properties.filterKeys { Patterns.wildcard(it, "sling.instance.$type.*.*") }.entries.fold(mutableMapOf(), { result, e ->
                val (key, value) = e
                val parts = key.substringAfter(".$type.").split(".")
                if (parts.size != 2) {
                    throw InstanceException("Instance list property '$key' has invalid format.")
                }

                val (name, prop) = parts

                result.getOrPut(name, { mutableMapOf() })[prop] = value as String
                result
            })
        }

        fun defaults(project: Project): List<RemoteInstance> {
            val config = SlingConfig.of(project)

            return listOf(RemoteInstance.create(URL_DEFAULT, { environment = config.environment }))
        }

        fun filter(project: Project): List<Instance> {
            return filter(project, SlingConfig.of(project).instanceName)
        }

        fun filter(project: Project, instanceFilter: String): List<Instance> {
            val config = SlingConfig.of(project)
            val all = config.instances.values

            // Specified by command line should not be filtered
            val cmd = all.filter { it.environment == Instance.ENVIRONMENT_CMD }
            if (cmd.isNotEmpty()) {
                return cmd
            }

            // Defined by build script, via properties or defaults are filterable by name
            return all.filter { Patterns.wildcards(it.name, instanceFilter) }
        }

        fun <T : Instance> filter(project: Project, type: KClass<T>): List<T> {
            return filter(project).filterIsInstance(type.java)
        }

        fun locals(project: Project): List<LocalInstance> {
            return filter(project, LocalInstance::class)
        }

        fun handles(project: Project): List<LocalHandle> {
            return Instance.locals(project).map { LocalHandle(project, it) }
        }

        fun remotes(project: Project): List<RemoteInstance> {
            return filter(project, RemoteInstance::class)
        }

    }

    val httpUrl: String

    val httpPort: Int
        get() = InstanceUrl.parse(httpUrl).httpPort

    @get:JsonIgnore
    val httpBasicAuthUrl: String
        get() = InstanceUrl.parse(httpUrl).httpBasicAuthUrl(user, password)

    val user: String

    val password: String

    @get:JsonIgnore
    val hiddenPassword: String
        get() = "*".repeat(password.length)

    val environment: String

    @get:JsonIgnore
    val cmd: Boolean
        get() = environment == ENVIRONMENT_CMD

    val type: String

    @get:JsonIgnore
    val credentials: String
        get() = "$user:$password"

    val name: String
        get() = "$environment-$type"

    fun validate() {
        if (!Formats.URL_VALIDATOR.isValid(httpUrl)) {
            throw SlingException("Malformed URL address detected in $this")
        }

        if (user.isBlank()) {
            throw SlingException("User cannot be blank in $this")
        }

        if (password.isBlank()) {
            throw SlingException("Password cannot be blank in $this")
        }

        if (environment.isBlank()) {
            throw SlingException("Environment cannot be blank in $this")
        }

        if (type.isBlank()) {
            throw SlingException("Type cannot be blank in $this")
        }
    }

}

val Collection<Instance>.names: String
    get() = joinToString(", ") { it.name }

fun Instance.isInitialized(project: Project): Boolean {
    return this !is LocalInstance || LocalHandle(project, this).initialized
}

fun Instance.isBeingInitialized(project: Project): Boolean {
    return this is LocalInstance && !LocalHandle(project, this).initialized
}