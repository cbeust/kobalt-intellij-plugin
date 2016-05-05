package com.beust.kobalt.intellij

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.HttpRequests
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * Our main application component, which just sees if our kobalt.jar file needs to be downloaded.
 *
 * @author Cedric Beust <cedric@beust.com>
 * @since 10 23, 2015
 */
class KobaltApplicationComponent : ApplicationComponent {
    override fun getComponentName() = "kobalt.ApplicationComponent"

    companion object {
        val LOG = Logger.getInstance(KobaltApplicationComponent::class.java)
        val threadPool = Executors.newFixedThreadPool(2)

        class CompletedFuture<T>(val value: T) : Future<T> {
            override fun get(timeout: Long, unit: TimeUnit?): T {
                throw UnsupportedOperationException()
            }

            override fun isDone(): Boolean {
                throw UnsupportedOperationException()
            }

            override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
                throw UnsupportedOperationException()
            }

            override fun isCancelled(): Boolean {
                throw UnsupportedOperationException()
            }

            override fun get(): T {
                return value
            }
        }

        internal val kobaltJar: Future<Path> by lazy {
            val path = findKobaltJar(KobaltApplicationComponent.version)
            val result =
                    if (!Constants.DEV_MODE) {
                        threadPool.submit(Callable {
                            DistributionDownloader.maybeDownloadAndInstallKobaltJarSilently() ?: path
                        }
                        )
                    } else {
                        CompletedFuture(path)
                    }
            result
        }

        private fun findKobaltJar(version: String) =
            if (Constants.DEV_MODE) {
                Paths.get(System.getProperty("user.home"), "kotlin/kobalt/kobaltBuild/libs/kobalt-$version.jar")
            } else {
                Paths.get(System.getProperty("user.home"),
                        ".kobalt/wrapper/dist/kobalt-$version/kobalt/wrapper/kobalt-$version.jar")
            }

        private val latestKobaltVersion: Future<String>
            get() {
                val callable = Callable<String> {
                    if (Constants.DEV_MODE) Constants.DEV_VERSION
                    else {
                        var result = Constants.MIN_KOBALT_VERSION
                        try {
                            result = HttpRequests.request(DistributionDownloader.RELEASE_URL)
                                    .productNameAsUserAgent().connect {request->
                                var version:String = Constants.MIN_KOBALT_VERSION
                                @Suppress("UNCHECKED_CAST")
                                val reader = BufferedReader(InputStreamReader(request.inputStream))
                                val jo = JsonParser().parse(reader) as JsonArray
                                if (jo.size() > 0) {
                                    var versionName = (jo.get(0) as JsonObject).get("name").asString
                                    if (versionName == null || versionName.isBlank()) {
                                        versionName = (jo.get(0) as JsonObject).get("tag_name").asString
                                    }
                                    if (versionName != null) {
                                        version = versionName
                                    }
                                }
                                version
                            }
                        } catch(ex: IOException) {
                            DistributionDownloader.warn(
                                    "Couldn't load the release URL: ${DistributionDownloader.RELEASE_URL}")
                        }
                        result
                    }
                }
                return Executors.newFixedThreadPool(1).submit(callable)
            }

        val version: String by lazy {
            try {
                latestKobaltVersion.get(20, TimeUnit.SECONDS)
            } catch(ex: Exception) {
                Constants.MIN_KOBALT_VERSION
            }

        }
    }

    override fun initComponent() {
    }

    override fun disposeComponent() {
        threadPool.shutdown()
    }

}
