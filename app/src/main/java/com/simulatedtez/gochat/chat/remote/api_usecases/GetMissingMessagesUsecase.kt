package com.simulatedtez.gochat.chat.remote.api_usecases

import com.simulatedtez.gochat.chat.remote.api_services.IChatApiService
import com.simulatedtez.gochat.chat.remote.models.ChatHistoryResponse
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.RemoteParams
import utils.ChatEndpointCaller
import utils.ResponseCallback

class GetMissingMessagesUsecase(
    private val params: GetMissingMessagesParams,
    private val chatApiService: IChatApiService
): ChatEndpointCaller<ChatHistoryResponse> {

    override suspend fun call(handler: ResponseCallback<ChatHistoryResponse>) {
        when (val res = chatApiService.getMissingMessages(params)) {
            is IResponse.Success -> {
                val chatServiceRes = ChatHistoryResponse(
                    data = res.data.data,
                    isSuccessful = true,
                    message = null,
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
    override val headers: Headers,
    override val request: Request
): RemoteParams(headers, request) {
    class Headers(
        val accessToken: String,
    )
    class Request(
        val chatReference: String,
        val yourUsername: String,
    )
}