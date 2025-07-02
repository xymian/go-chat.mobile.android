package com.simulatedtez.gochat.remote

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

class Response<R>(val call: suspend () -> HttpResponse): IResponse<R> {

    suspend inline operator fun <reified R> invoke(): IResponse<R> = try {
        val response = call()
        val data: R = response.body()
        if (response.status.isSuccess()) {
            IResponse.Success(
                data = data
            )
        } else {
            IResponse.Failure(reason = response.status.description, response = response.bodyAsText())
        }
    } catch (e: Exception) {
        IResponse.Failure(exception = e, e.message ?: "unknown reason :(")
    }
}

sealed interface IResponse<R> {
    data class Success<R>(
        val data: R
    ): IResponse<R>

    data class Failure<R>(
        val exception: Exception? = null,
        val reason: String,
        val response: String? = null
    ): IResponse<R>
}