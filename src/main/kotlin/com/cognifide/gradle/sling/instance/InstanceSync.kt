package com.cognifide.gradle.sling.instance

import com.cognifide.gradle.sling.api.SlingConfig
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

    fun delete(url: String): String {
        return fetch(HttpDelete(normalizeUrl(url)))
    }

    fun put(url: String): String {
        return fetch(HttpPut(normalizeUrl(url)))
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
                throw DeployException("Unexpected instance response: ${response.statusLine}")
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
            throw DeployException("Failed instance request: ${e.message}", e)
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
                ?: throw DeployException("Package is not uploaded on Sling instance.")

        return pkg.path
    }

    fun determineRemotePackage(file: File): Package? {
        if (!ZipUtil.containsEntry(file, PackagePlugin.VLT_PROPERTIES)) {
            throw DeployException("File is not a valid Vault package: $file")
        }

        val xml = ZipUtil.unpackEntry(file, PackagePlugin.VLT_PROPERTIES).toString(Charsets.UTF_8)
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())

        val group = doc.select("entry[key=group]").text()
        val name = doc.select("entry[key=name]").text()
        val version = doc.select("entry[key=version]").text()

        return resolveRemotePackage(group, name, version)
    }

    private fun resolveRemotePackage(group: String, name: String, version: String): Package? {
        val url = "${instance.httpUrl}/bin/cpm/package.list.json"

        logger.debug("Asking for uploaded packages using URL: '$url'")
        val packages = try {
            ListResponse.fromJson(get(url))
        } catch (e: Exception) {
            throw DeployException("Cannot determine remote package!", e)
        }

        return packages.find { pkg ->
            pkg.definition.let { def ->
                def.group == group && def.name == name && def.version == version
            }
        }
    }

    fun uploadPackage(file: File): PackageResponse {
        lateinit var exception: DeployException
        for (i in 0..config.uploadRetryTimes) {
            try {
                return uploadPackageOnce(file)
            } catch (e: DeployException) {
                exception = e

                if (i < config.uploadRetryTimes) {
                    logger.warn("Cannot upload package to $instance.")

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
            throw DeployException(String.format("Package file '%s' not found!", file.path), e)
        } catch (e: Exception) {
            throw DeployException("Cannot upload package.", e)
        }

        val response = UploadResponse.fromJson(json)
        if (!response.success) {
            throw DeployException("Upload ended with status: ${response.status}")
        }

        return response

    }

    fun installPackage(uploadedPackagePath: String): PackageResponse {
        lateinit var exception: DeployException
        for (i in 0..config.installRetryTimes) {
            try {
                return installPackageOnce(uploadedPackagePath)
            } catch (e: DeployException) {
                exception = e
                if (i < config.installRetryTimes) {
                    logger.warn("Cannot install package on $instance.")

                    val header = "Retrying install (${i + 1}/${config.installRetryTimes}) after delay."
                    val countdown = ProgressCountdown(project, header, config.installRetryDelay)
                    countdown.run()
                }
            }
        }

        throw exception
    }

    fun installPackageOnce(uploadedPackagePath: String): PackageResponse {
        val url = "${instance.httpUrl}/bin/cpm/package.install.json"

        logger.info("Installing package using command: $url")

        val json = try {
            postUrlencoded(url, mapOf("path" to uploadedPackagePath))
        } catch (e: Exception) {
            throw DeployException("Cannot install package.", e)
        }
        val response = InstallResponse.fromJson(json)
        if (!response.success) {
            throw DeployException("Install ended with status: ${response.status}")
        }

        return response
    }

    fun isSnapshot(file: File): Boolean {
        return Patterns.wildcard(file, config.packageSnapshots)
    }

    fun deployPackage(file: File) {
        installPackage(uploadPackage(file).path)
    }

    fun deletePackage(path: String): DeleteResponse {
        val url = "${instance.httpUrl}/bin/cpm/package.delete.json?path=$path"

        logger.info("Deleting package using command: $url")

        val json = try {
            delete(url)
        } catch (e: Exception) {
            throw DeployException("Cannot delete package.", e)
        }

        val response = DeleteResponse.fromJson(json)
        if (!response.success) {
            throw DeployException("Uninstall ended with status: ${response.status}")
        }

        return response
    }

    fun uninstallPackage(installedPackagePath: String): PackageResponse {
        val url = "${instance.httpUrl}/bin/cpm/package.uninstall.json"

        logger.info("Uninstalling package using command: $url")

        val json = try {
            postUrlencoded(url, mapOf("path" to installedPackagePath))
        } catch (e: Exception) {
            throw DeployException("Cannot uninstall package.", e)
        }

        val response = UninstallResponse.fromJson(json)
        if (!response.success) {
            throw DeployException("Uninstall ended with status: ${response.status}")
        }

        return response
    }

    fun determineInstanceState(): InstanceState {
        return InstanceState(this, instance)
    }

    fun determineBundleState(): BundleState {
        logger.debug("Asking Sling for OSGi bundles using URL: '$bundlesUrl'")

        return try {
            BundleState.fromJson(get(bundlesUrl))
        } catch (e: Exception) {
            logger.debug("Cannot determine OSGi bundles state on $instance", e)
            BundleState.unknown(e)
        }
    }

    fun determineComponentState(): ComponentState {
        logger.debug("Asking Sling for OSGi components using URL: '$bundlesUrl'")

        return try {
            ComponentState.fromJson(get(componentsUrl))
        } catch (e: Exception) {
            logger.debug("Cannot determine OSGi components state on $instance", e)
            ComponentState.unknown()
        }
    }

    fun reload() {
        try {
            logger.info("Triggering instance(s) shutdown")
            postUrlencoded(vmStatUrl, mapOf("shutdown_type" to "Restart"))
        } catch (e: DeployException) {
            throw InstanceException("Cannot trigger shutdown for instance $instance", e)
        }
    }

}

fun Collection<Instance>.sync(project: Project, callback: (InstanceSync) -> Unit) {
    return map { InstanceSync(project, it) }.parallelStream().forEach(callback)
}