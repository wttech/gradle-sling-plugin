package com.cognifide.gradle.sling.instance

import com.cognifide.gradle.sling.api.SlingConfig
import com.cognifide.gradle.sling.internal.BuildCache
import com.cognifide.gradle.sling.internal.Patterns
import com.cognifide.gradle.sling.internal.ProgressCountdown
import com.cognifide.gradle.sling.internal.http.PreemptiveAuthInterceptor
import com.cognifide.gradle.sling.pkg.PackagePlugin
import com.cognifide.gradle.sling.pkg.deploy.*
import org.apache.commons.io.IOUtils
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.NameValuePair
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.*
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.message.BasicNameValuePair
import org.apache.http.ssl.SSLContextBuilder
import org.gradle.api.Project
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.io.FileNotFoundException
import java.util.*

class InstanceSync(val project: Project, val instance: Instance) {

    val config = SlingConfig.of(project)

    val logger = project.logger

    val bundlesUrl = "${instance.httpUrl}/system/console/bundles.json"

    val componentsUrl = "${instance.httpUrl}/system/console/components.json"

    val vmStatUrl = "${instance.httpUrl}/system/console/vmstat"

    var basicUser = instance.user

    var basicPassword = instance.password

    var connectionTimeout = config.instanceConnectionTimeout

    var connectionUntrustedSsl = config.instanceConnectionUntrustedSsl

    var connectionRetries = true

    var requestConfigurer: (HttpRequestBase) -> Unit = { _ -> }

    var responseHandler: (HttpResponse) -> Unit = { _ -> }

    fun get(url: String): String {
        return fetch(HttpGet(normalizeUrl(url)))
    }

    fun head(url: String): String {
        return fetch(HttpHead(normalizeUrl(url)))
    }

    fun delete(url: String): String {
        return fetch(HttpDelete(normalizeUrl(url)))
    }

    fun put(url: String): String {
        return fetch(HttpPut(normalizeUrl(url)))
    }

    fun patch(url: String): String {
        return fetch(HttpPatch(normalizeUrl(url)))
    }

    fun postUrlencoded(url: String, params: Map<String, Any> = mapOf()): String {
        return post(url, createEntityUrlencoded(params))
    }

    fun postMultipart(url: String, params: Map<String, Any> = mapOf()): String {
        return post(url, createEntityMultipart(params))
    }

    private fun post(url: String, entity: HttpEntity): String {
        return fetch(HttpPost(normalizeUrl(url)).apply { this.entity = entity })
    }

    /**
     * Fix for HttpClient's: 'escaped absolute path not valid'
     * https://stackoverflow.com/questions/13652681/httpclient-invalid-uri-escaped-absolute-path-not-valid
     */
    private fun normalizeUrl(url: String): String {
        return url.replace(" ", "%20")
    }

    fun fetch(method: HttpRequestBase): String {
        return execute(method, { response ->
            val body = IOUtils.toString(response.entity.content) ?: ""

            if (response.statusLine.statusCode == HttpStatus.SC_OK) {
                return@execute body
            } else {
                logger.debug(body)
                throw DeployException("Unexpected response from $instance: ${response.statusLine}")
            }
        })
    }

    fun <T> execute(method: HttpRequestBase, success: (HttpResponse) -> T): T {
        try {
            requestConfigurer(method)

            val client = createHttpClient()
            val response = client.execute(method)

            responseHandler(response)

            return success(response)
        } catch (e: Exception) {
            throw DeployException("Failed request to $instance: ${e.message}", e)
        } finally {
            method.releaseConnection()
        }
    }

    fun createHttpClient(): HttpClient {
        val builder = HttpClientBuilder.create()
                .addInterceptorFirst(PreemptiveAuthInterceptor())
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(connectionTimeout)
                        .setConnectionRequestTimeout(connectionTimeout)
                        .build()
                )
                .setDefaultCredentialsProvider(BasicCredentialsProvider().apply {
                    setCredentials(AuthScope.ANY, UsernamePasswordCredentials(basicUser, basicPassword))
                })
        if (connectionUntrustedSsl) {
            builder.setSSLSocketFactory(createSslConnectionSocketFactory())
        }
        if (!connectionRetries) {
            builder.disableAutomaticRetries()
        }

        return builder.build()
    }

    private fun createSslConnectionSocketFactory(): SSLConnectionSocketFactory {
        val sslContext = SSLContextBuilder()
                .loadTrustMaterial(null, { _, _ -> true })
                .build()
        return SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE)
    }

    private fun createEntityUrlencoded(params: Map<String, Any>): HttpEntity {
        return UrlEncodedFormEntity(params.entries.fold(ArrayList<NameValuePair>(), { result, e ->
            result.add(BasicNameValuePair(e.key, e.value.toString())); result
        }))
    }

    private fun createEntityMultipart(params: Map<String, Any>): HttpEntity {
        val builder = MultipartEntityBuilder.create()
        for ((key, value) in params) {
            if (value is File) {
                if (value.exists()) {
                    builder.addBinaryBody(key, value)
                }
            } else {
                val str = value.toString()
                if (str.isNotBlank()) {
                    builder.addTextBody(key, str)
                }
            }
        }

        return builder.build()
    }

    fun determineRemotePackage(): Package? {
        val group = project.group.toString()
        val name = SlingConfig.of(project).packageName
        val version = project.version.toString()

        return resolveRemotePackage(group, name, version)
    }

    fun determineRemotePackagePath(): String {
        if (!config.packageRemotePath.isBlank()) {
            return config.packageRemotePath
        }

        val pkg = determineRemotePackage()
                ?: throw DeployException("Package is not uploaded on $instance.")

        return pkg.path
    }

    fun determineRemotePackage(file: File, refresh: Boolean = true): Package? {
        if (!ZipUtil.containsEntry(file, PackagePlugin.VLT_PROPERTIES)) {
            throw DeployException("File is not a valid Vault package: $file")
        }

        val xml = ZipUtil.unpackEntry(file, PackagePlugin.VLT_PROPERTIES).toString(Charsets.UTF_8)
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())

        val group = doc.select("entry[key=group]").text()
        val name = doc.select("entry[key=name]").text()
        val version = doc.select("entry[key=version]").text()

        return resolveRemotePackage(group, name, version, refresh)
    }

    private fun resolveRemotePackage(group: String, name: String, version: String, refresh: Boolean = true): Package? {
        val packages: List<Package> = BuildCache.of(project).getOrPut("${instance.name}.packages", {
            val url = "${instance.httpUrl}/bin/cpm/package.list.json"

            logger.debug("Asking for uploaded packages using URL: '$url'")

            try {
                ListResponse.fromJson(get(url))
            } catch (e: Exception) {
                throw DeployException("Malformed response after listing packages on instance $instance.", e)
            }
        }, refresh)

        return packages.find { it.definition.check(group, name, version) }
    }

    fun uploadPackage(file: File): PackageResponse {
        lateinit var exception: DeployException
        for (i in 0..config.uploadRetryTimes) {
            try {
                return uploadPackageOnce(file)
            } catch (e: DeployException) {
                exception = e

                if (i < config.uploadRetryTimes) {
                    logger.warn("Cannot upload package $file to $instance.")
                    logger.debug("Upload error", e)

                    val header = "Retrying upload (${i + 1}/${config.uploadRetryTimes}) after delay."
                    val countdown = ProgressCountdown(project, header, config.uploadRetryDelay)
                    countdown.run()
                }
            }
        }

        throw exception
    }

    fun uploadPackageOnce(file: File): PackageResponse {
        val url = "${instance.httpUrl}/bin/cpm/package.upload.json"

        logger.info("Uploading package at path '{}' to URL '{}'", file.path, url)

        val json = try {
            postMultipart(url, mapOf(
                    "file" to file,
                    "force" to (config.uploadForce || isSnapshot(file))
            ))
        } catch (e: FileNotFoundException) {
            throw DeployException("Package file $file to be uploaded not found!", e)
        } catch (e: Exception) {
            throw DeployException("Cannot upload package $file to instance $instance. Reason: request failed.", e)
        }

        val response = try {
            UploadResponse.fromJson(json)
        } catch (e: Exception) {
            throw DeployException("Malformed response after uploading package $file to instance $instance.", e)
        }

        if (!response.success) {
            throw DeployException("Cannot upload package $file to $instance. Reason: ${response.status}.")
        }

        return response
    }

    fun installPackage(remotePath: String): PackageResponse {
        lateinit var exception: DeployException
        for (i in 0..config.installRetryTimes) {
            try {
                return installPackageOnce(remotePath)
            } catch (e: DeployException) {
                exception = e
                if (i < config.installRetryTimes) {
                    logger.warn("Cannot install package $remotePath on $instance.")
                    logger.debug("Install error", e)

                    val header = "Retrying install (${i + 1}/${config.installRetryTimes}) after delay."
                    val countdown = ProgressCountdown(project, header, config.installRetryDelay)
                    countdown.run()
                }
            }
        }

        throw exception
    }

    fun installPackageOnce(remotePath: String): PackageResponse {
        val url = "${instance.httpUrl}/bin/cpm/package.install.json"

        logger.info("Installing package using command: $url")

        val json = try {
            postUrlencoded(url, mapOf("path" to remotePath))
        } catch (e: Exception) {
            throw DeployException("Cannot install package. Reason: request failed.", e)
        }

        val response = try {
            InstallResponse.fromJson(json)
        } catch (e: Exception) {
            throw DeployException("Malformed response after installing package $remotePath on $instance.", e)
        }

        if (!response.success) {
            throw DeployException("Cannot install package. Reason: ${response.status}.")
        }

        return response
    }

    fun isSnapshot(file: File): Boolean {
        return Patterns.wildcard(file, config.packageSnapshots)
    }

    fun deployPackage(file: File) {
        installPackage(uploadPackage(file).path)
    }

    fun deletePackage(remotePath: String): DeleteResponse {
        val url = "${instance.httpUrl}/bin/cpm/package.delete.json?path=$remotePath"

        logger.info("Deleting package using command: $url")

        val json = try {
            delete(url)
        } catch (e: Exception) {
            throw DeployException("Cannot delete package $remotePath from $instance. Reason: request failed.", e)
        }

        val response = try {
            DeleteResponse.fromJson(json)
        } catch (e: Exception) {
            throw DeployException("Malformed response after deleting package $remotePath from $instance.", e)
        }

        if (!response.success) {
            throw DeployException("Cannot delete package $remotePath from $instance. Reason: ${response.status}")
        }

        return response
    }

    fun uninstallPackage(remotePath: String): PackageResponse {
        val url = "${instance.httpUrl}/bin/cpm/package.uninstall.json"

        logger.info("Uninstalling package using command: $url")

        val json = try {
            postUrlencoded(url, mapOf("path" to remotePath))
        } catch (e: Exception) {
            throw DeployException("Cannot uninstall package $remotePath from $instance. Reason: request failed.", e)
        }

        val response = try {
            UninstallResponse.fromJson(json)
        } catch (e: Exception) {
            throw DeployException("Malformed response after uninstalling package $remotePath from $instance.", e)
        }

        if (!response.success) {
            throw DeployException("Cannot uninstall package $remotePath from $instance. Reason: ${response.status}.")
        }

        return response
    }

    fun determineInstanceState(): InstanceState {
        return InstanceState(this, instance)
    }

    fun determineBundleState(): BundleState {
        logger.debug("Asking for OSGi bundles using URL: '$bundlesUrl'")

        return try {
            BundleState.fromJson(get(bundlesUrl))
        } catch (e: Exception) {
            logger.debug("Cannot determine OSGi bundles state on $instance", e)
            BundleState.unknown(e)
        }
    }

    fun determineComponentState(): ComponentState {
        logger.debug("Asking for OSGi components using URL: '$bundlesUrl'")

        return try {
            ComponentState.fromJson(get(componentsUrl))
        } catch (e: Exception) {
            logger.debug("Cannot determine OSGi components state on $instance", e)
            ComponentState.unknown()
        }
    }

    fun reload() {
        try {
            logger.info("Triggering shutdown of $instance")
            postUrlencoded(vmStatUrl, mapOf("shutdown_type" to "Restart"))
        } catch (e: DeployException) {
            throw InstanceException("Cannot trigger shutdown of $instance", e)
        }
    }

}

fun Collection<Instance>.sync(project: Project, callback: (InstanceSync) -> Unit) {
    return map { InstanceSync(project, it) }.parallelStream().forEach(callback)
}