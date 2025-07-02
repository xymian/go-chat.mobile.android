package com.simulatedtez.gochat.chat.remote.api_usecases

import com.simulatedtez.gochat.chat.remote.models.ChatHistoryResponse
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.Response
import kotlinx.serialization.json.Json
import utils.ChatEndpointCaller
import utils.ResponseCallback

class GetMissingMessagesUsecase(
    private val params: GetMissingMessagesParams,
    private val missingMessagesEndpoint: suspend (params: GetMissingMessagesParams) -> IResponse<List<Message>>
): ChatEndpointCaller<ChatHistoryResponse> {

    override suspend fun call(handler: ResponseCallback<ChatHistoryResponse>) {
        when (val res = missingMessagesEndpoint(params)) {
            is IResponse.Success -> {
                val chatServiceRes = ChatHistoryResponse(
                    data = res.data,
                    isSuccessful = true,
                    error = null,
                )
                handler.onResponse(chatServiceRes)
            }
            else -> {
                handler.onFailure((res as IResponse.Failure<List<Message>>).exception)
            }
        }
    }
}

data class GetMissingMessagesParams(
    val headers: Headers,
    val params: Params
) {
    class Headers(
        val token: String,
    )
    class Params(
        val chatReference: String,
        val yourUsername: String,
    )
}