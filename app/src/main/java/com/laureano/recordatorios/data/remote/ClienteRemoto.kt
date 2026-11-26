package com.laureano.recordatorios.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ClienteRemoto {

    fun crearApi(
        urlBase: String,
        registrarPeticiones: Boolean = false,
        timeoutSegundos: Long = 5
    ): RecordatorioApi {
        val cliente = OkHttpClient.Builder()
            .connectTimeout(timeoutSegundos, TimeUnit.SECONDS)
            .readTimeout(timeoutSegundos, TimeUnit.SECONDS)
            .writeTimeout(timeoutSegundos, TimeUnit.SECONDS)
            .apply {
                if (registrarPeticiones) {
                    addInterceptor(
                        HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC)
                    )
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(urlBase)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RecordatorioApi::class.java)
    }
}
