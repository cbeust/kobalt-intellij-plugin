package com.beust.kobalt.intellij.server

import com.beust.kobalt.intellij.GetDependenciesData
import com.intellij.openapi.diagnostic.Logger
import okhttp3.OkHttpClient
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
class ServerFacade {
    companion object {
        val LOG = Logger.getInstance(ServerFacade::class.java)
    }

    private interface ServerApi {
        @GET("/v0/getDependencies") fun getDependencies(@Query("buildFile") buildFile: String): Call<GetDependenciesData>
        @GET("/quit") fun quit(): Call<Any>
    }


    fun sendQuitCommand() {
        retrofitBuilder(ServerUtil.findServerPort())
                .create(ServerApi::class.java)
                .quit()
                .execute()
    }

    fun sendGetDependencies(pathToBuildFile: String)
            = retrofitBuilder(ServerUtil.findServerPort())
            .create(ServerApi::class.java)
            .getDependencies(pathToBuildFile)
            .execute()

    private fun retrofitBuilder(port: Int?): Retrofit {
        return Retrofit.Builder()
                .client(OkHttpClient.Builder()
                        .connectTimeout(3, TimeUnit.MINUTES)
                        .readTimeout(3, TimeUnit.MINUTES)
                        .build())
                .baseUrl("http://localhost:$port")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }
}