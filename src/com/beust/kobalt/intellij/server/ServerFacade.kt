package com.beust.kobalt.intellij.server

import com.beust.kobalt.intellij.GetDependenciesData
import com.intellij.openapi.diagnostic.Logger
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * @author Dmitry Zhuravlev
 *         Date: 08.05.16
 */
class ServerFacade(val port: Int?) {
    companion object {
        val LOG = Logger.getInstance(ServerFacade::class.java)
    }

    private interface ServerApi {
        @GET("/v0/getDependencies") fun getDependencies(@Query("buildFile") buildFile: String): Call<GetDependenciesData>
        @GET("/quit") fun quit(): Call<ResponseBody>
        @GET("/ping") fun ping(): Call<ResponseBody>
    }

    fun sendPingCommand() = buildService().ping().execute()

    fun sendQuitCommand() = buildService().quit().execute()

    fun sendGetDependencies(pathToBuildFile: String) =
            buildService().getDependencies(pathToBuildFile).execute()

    private fun buildService(): ServerApi {
        return Retrofit.Builder()
                .client(OkHttpClient.Builder()
                        .connectTimeout(3, TimeUnit.MINUTES)
                        .readTimeout(3, TimeUnit.MINUTES)
                        .build())
                .baseUrl("http://localhost:$port")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ServerApi::class.java)
    }
}