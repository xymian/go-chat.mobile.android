package com.simulatedtez.gochat.chat.remote.api_usecases

import ChatServiceErrorResponse
import com.simulatedtez.gochat.chat.remote.api_services.IChatApiService
import com.simulatedtez.gochat.chat.remote.models.ChatHistoryResponse
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.RemoteParams
import io.ktor.http.HttpStatusCode
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
                val error = (res as IResponse.Failure<ParentResponse<List<Message>>>)
                handler.onFailure(
                    ChatServiceErrorResponse(
                        statusCode = error.response?.statusCode ?: HttpStatusCode.NotFound.value,
                        exception = error.exception,
                        reason = error.reason,
                        message = ""
                    )
                )
            }
        }
    }
}

data class GetMissingMessagesParams(
    override val request: Request
): RemoteParams(request = request) {
    class Request(
        val chatReference: String,
        val yourUsername: String,
    )
}