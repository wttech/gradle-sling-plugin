package com.cognifide.gradle.sling.internal

import org.gradle.api.Project

class BuildCache {

    private val cache = mutableMapOf<String, Any>()

    @Suppress("unchecked_cast")
    fun <T> get(key: String): Any? {
        return cache[key] as T
    }

    @Suppress("unchecked_cast")
    fun <T> getOrPut(key: String, defaultValue: () -> Any, invalidate: Boolean = false): T {
        return if (invalidate) {
            val value = defaultValue()
            put(key, value)
            value as T
        } else {
            cache.getOrPut(key, defaultValue) as T
        }
    }

    fun put(key: String, value: Any) {
        cache[key] = value
    }

    companion object {

        fun of(project: Project): BuildCache {
            val ext = project.rootProject.extensions.extraProperties
            val key = BuildCache::class.java.canonicalName
            if (!ext.has(key)) {
                ext.set(key, BuildCache())
            }

            return ext.get(key) as BuildCache
        }

    }

}