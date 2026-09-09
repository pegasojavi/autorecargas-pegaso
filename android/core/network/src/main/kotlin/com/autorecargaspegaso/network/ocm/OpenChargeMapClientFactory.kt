package com.autorecargaspegaso.network.ocm

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Composición manual del cliente (sin Hilt todavía — CLAUDE.md sección 4
 * pide Hilt, pero se difiere hasta fijar versiones compatibles con AGP
 * 9.0.1/Kotlin 2.3.20; ver nota en el informe de scaffolding). El `app`
 * module llama a [create] una vez y reutiliza la instancia.
 */
object OpenChargeMapClientFactory {

    fun create(debugLogging: Boolean = false): OpenChargeMapApi {
        val json = Json { ignoreUnknownKeys = true }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .apply {
                if (debugLogging) {
                    addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                }
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(OpenChargeMapApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(OpenChargeMapApi::class.java)
    }
}
