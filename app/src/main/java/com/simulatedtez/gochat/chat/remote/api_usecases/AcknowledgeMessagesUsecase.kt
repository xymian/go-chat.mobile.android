package com.simulatedtez.gochat.chat.remote.api_usecases

import com.simulatedtez.gochat.chat.remote.models.AckResponse
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.chat.view_model.models.ChatInfo
import com.simulatedtez.gochat.remote.IResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import utils.ChatEndpointCaller
import utils.ChatEndpointCallerWithData
import utils.ResponseCallback

class AcknowledgeMessagesUsecase(
    private val chatInfo: ChatInfo,
    private val ackEndpoint: suspend (request: AckParams) -> IResponse<String>
): ChatEndpointCallerWithData<List<Message>, AckResponse> {

    override suspend fun call(data: List<Message>?, handler: ResponseCallback<AckResponse>) {
        data?.let { messages ->
            val ackParams = createAckParams(messages.sortedBy { it.messageTimestamp })
            call(ackParams, handler)
        }
    }

    private suspend fun call(
        request: AckParams,
        chatServiceHandler: ResponseCallback<AckResponse>
    ) {
        val res = ackEndpoint(request)
        if (res is IResponse.Success) {
            chatServiceHandler.onResponse(AckResponse(
                data = res.data,
                isSuccessful = true,
                error = null
            ))
        } else {
            chatServiceHandler.onFailure(null)
        }
    }

    private fun createAckParams(messages: List<Message>): AckParams {
        val sortedList = messages.sortedBy { it.messageTimestamp }
        return AckParams(
            headers = AckParams.Headers(token = ""),
            params = AckParams.Params(
                from = sortedList.first().messageTimestamp!!,
                to = sortedList.last().messageTimestamp!!,
                chatReference = chatInfo.chatReference,
                yourUsername = chatInfo.username
            )
        )
    }

    data class AckParams(
        val headers: Headers,
        val params: Params
    ) {
        class Headers(
            val token: String,
        )

        @Serializable
        class Params(
            @SerialName("from")
            val from: String,
            @SerialName("to")
            val to: String,
            @SerialName("chatReference")
            val chatReference: String,
            @SerialName("username")
            val yourUsername: String,
        )
    }
}