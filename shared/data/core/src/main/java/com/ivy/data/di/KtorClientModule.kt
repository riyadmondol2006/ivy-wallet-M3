package com.ivy.data.di

import android.content.Context
import android.content.pm.ApplicationInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object KtorClientModule {
    @Provides
    @Singleton
    fun provideKtorClient(
        @ApplicationContext context: Context,
        json: Json,
    ): HttpClient {
        val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return HttpClient {
            install(ContentNegotiation) {
                json(json)
            }

            install(Logging) {
                // Request bodies carry the user's full financial backup and bearer token. Log
                // only headers in debug builds and nothing at all in release builds.
                level = if (debuggable) LogLevel.HEADERS else LogLevel.NONE
                logger = object : Logger {
                    override fun log(message: String) {
                        Timber.d(message)
                    }
                }
                sanitizeHeader { header -> header == io.ktor.http.HttpHeaders.Authorization }
            }
        }
    }
}
