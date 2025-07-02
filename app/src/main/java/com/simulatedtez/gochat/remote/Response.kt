package com.simulatedtez.gochat.remote

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
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
            IResponse.Failure(reason = FailureReason.GENERIC)
        }
    } catch (e: Exception) {
        IResponse.Failure(e, FailureReason.GENERIC, e.message)
    }
}

sealed interface IResponse<R> {
    data class Success<R>(
        val data: R
    ): IResponse<R>

    data class Failure<R>(
        val exception: Throwable? = null,
        val reason: FailureReason,
        val response: String? = null
    ): IResponse<R>
}

enum class FailureReason {
    GENERIC
}