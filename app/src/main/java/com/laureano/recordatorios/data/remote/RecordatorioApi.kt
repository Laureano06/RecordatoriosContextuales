package com.laureano.recordatorios.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface RecordatorioApi {
    @GET("recordatorios")
    suspend fun listar(): Response<List<RecordatorioDto>>

    // El servicio usa el uuid para no crear dos veces el mismo recordatorio
    @POST("recordatorios")
    suspend fun crear(@Body recordatorio: RecordatorioDto): Response<RecordatorioDto>

    @PUT("recordatorios/{id}")
    suspend fun actualizar(
        @Path("id") id: Long,
        @Body recordatorio: RecordatorioDto
    ): Response<RecordatorioDto>

    @DELETE("recordatorios/{id}")
    suspend fun eliminar(@Path("id") id: Long): Response<Unit>
}
