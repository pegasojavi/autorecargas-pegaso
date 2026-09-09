package com.autorecargaspegaso.network.geocoding

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object NominatimClientFactory {

    /** Exigido por la política de uso de Nominatim — identifica la app, no un navegador genérico. */
    const val USER_AGENT = "AutoRecargasPegaso/1.0 (Android)"

    fun create(): NominatimApi {
        val json = Json { ignoreUnknownKeys = true }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(NominatimApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(NominatimApi::class.java)
    }
}
