package com.voca.mobile.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.voca.mobile.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit

/** In-memory mirror of the persisted token so the OkHttp interceptor can read it synchronously. */
object SessionManager {
    @Volatile
    var token: String? = null

    /** Set by the app to react to a 401 (clear token + bounce to auth). */
    @Volatile
    var onUnauthorized: (() -> Unit)? = null
}

private class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
            .header("Accept", "application/json")
        SessionManager.token?.takeIf { it.isNotEmpty() }?.let {
            builder.header("Authorization", "Bearer $it")
        }
        val response = chain.proceed(builder.build())
        if (response.code == 401) {
            SessionManager.onUnauthorized?.invoke()
        }
        return response
    }
}

object NetworkModule {

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    val api: ApiService by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.VOCA_API_URL.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
    }
}
