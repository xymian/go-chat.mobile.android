package com.simulatedtez.gochat.remote

import com.simulatedtez.gochat.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

suspend fun HttpClient.postWithBaseUrl(endpoint: String, block: HttpRequestBuilder.() -> Unit): HttpResponse {
    return post(BuildConfig.CHAT_HISTORY_BASE_URL + endpoint) {
        block()
    }
}