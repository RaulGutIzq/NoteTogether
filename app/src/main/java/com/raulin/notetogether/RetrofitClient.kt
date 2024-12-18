package com.raulin.notetogether
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

object RetrofitClient {

    private const val BASE_URL = "https://sandbox.phpulse.es/" // Reemplaza con la URL base de tu API

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Nivel de logging para depuración
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor) // Añade el interceptor para logs
        .build()

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Convierte JSON automáticamente
            .client(client) // Cliente HTTP configurado
            .build()
    }

}