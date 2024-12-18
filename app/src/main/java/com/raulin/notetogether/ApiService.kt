package com.raulin.notetogether

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Query
import Usuarios


interface ApiService {

    @GET("?action=usuarios&{id}") // Ejemplo de endpoint
    fun getUser(@Path("id") userId: Int): Call<Usuarios> // Define el tipo de retorno


    @GET("?action=usuarios") // Ejemplo de endpoint
    fun getUserS():  Call<List<Usuarios>> // Define el tipo de retorno

    @DELETE("?action=usuarios") // Define la ruta sin la llave en la URL
    fun delUser(@Query("id") userId: Int): Call<Void> // Usa @Query para parámetros de consulta

    @POST("?action=usuarios")
    fun addUser(
        @Body usuario: Usuarios
    ): Call<Void>

    // Actualizar un usuario existente (PUT)
    @PUT("?action=usuarios")
    fun updateUser(
        @Query("id") userId: Int,
        @Body usuario: Usuarios
    ): Call<Void>


}