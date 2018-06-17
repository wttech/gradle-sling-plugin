package com.cognifide.gradle.sling.instance.satisfy

import com.cognifide.gradle.sling.api.SlingConfig
import com.cognifide.gradle.sling.instance.Instance
import com.cognifide.gradle.sling.instance.InstanceSync
import com.cognifide.gradle.sling.instance.action.AbstractAction
import com.cognifide.gradle.sling.instance.action.AwaitAction
import com.cognifide.gradle.sling.instance.action.ReloadAction
import com.cognifide.gradle.sling.internal.Patterns
import com.cognifide.gradle.sling.internal.file.resolver.FileGroup
import com.cognifide.gradle.sling.internal.file.resolver.FileResolution
import com.cognifide.gradle.sling.internal.file.resolver.FileResolver
import groovy.lang.Closure
import org.gradle.util.ConfigureUtil
import java.io.File

class PackageGroup(resolver: FileResolver, name: String) : FileGroup(resolver, name) {

    private val project = resolver.project

    private val config = SlingConfig.of(project)

    /**
     * Instances involved in packages deployment.
     */
    val instances by lazy {
        if (config.deployDistributed) {
            Instance.filter(project, config.instanceAuthorName)
        } else {
            Instance.filter(project)
        }.filter { Patterns.wildcard(it.name, instanceName) }
    }

    /**
     * Instance name filter for excluding group from deployment.
     */
    var instanceName = "*"

    /**
     * Hook for preparing instance before deploying packages
     */
    var initializer: (InstanceSync) -> Unit = {}

    /**
     * Hook for cleaning instance after deploying packages
     */
    var finalizer: (InstanceSync) -> Unit = {}

    /**
     * Hook after deploying all packages to all instances called only when
     * at least one package was deployed on any instance.
     */
    var completer: () -> Unit = { await() }

    fun await() {
        await({})
    }

    fun await(configurer: Closure<*>) {
        await({ ConfigureUtil.configure(configurer, this) })
    }

    fun await(configurer: AwaitAction.() -> Unit) {
        action(AwaitAction(project, instances), configurer)
    }

    fun reload() {
        reload({})
    }

    fun reload(configurer: Closure<*>) {
        reload({ ConfigureUtil.configure(configurer, this) })
    }

    fun reload(configurer: ReloadAction.() -> Unit) {
        action(ReloadAction(project, instances), configurer)
    }

    private fun <T : AbstractAction> action(action: T, configurer: T.() -> Unit) {
        action.apply { notify = false }.apply(configurer).perform()
    }

    override fun createResolution(id: String, resolver: (FileResolution) -> File): FileResolution {
        return PackageResolution(this, id, resolver)
    }

}
