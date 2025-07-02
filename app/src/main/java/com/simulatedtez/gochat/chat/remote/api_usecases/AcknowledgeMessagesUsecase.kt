package com.simulatedtez.gochat.chat.remote.api_usecases

import com.simulatedtez.gochat.chat.remote.api_services.IChatApiService
import com.simulatedtez.gochat.chat.remote.models.AckResponse
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.remote.IEndpointCaller
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.Response
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import utils.ChatEndpointCaller

class AcknowledgeMessagesUsecase(private val chatApiService: IChatApiService):
    IAcknowledgeMessageEndpointCaller {

    override suspend fun call(data: List<Message>?, handler: ChatEndpointCaller.ResponseCallback<AckResponse>) {
        data?.let { messages ->
            val handler1 = object : IResponseHandler<String, IResponse<String>> {
                override fun onResponse(response: IResponse<String>) {

                }
            }
            val ackParams = getSupportData(messages.sortedBy { it.messageTimestamp })
            call(ackParams, messages, handler1, handler)
        }
    }

    override suspend fun call(
        request: AckParams,
        data: List<Message>?,
        handler1: IResponseHandler<String, IResponse<String>>,
        handler2: ChatEndpointCaller.ResponseCallback<AckResponse>
    ) {
        val res = chatApiService.acknowledgeMessage(request)
        handler1.onResponse(res)
        if (res is IResponse.Success) {
            handler2.onResponse(AckResponse(
                data = res.data,
                isSuccessful = true,
                error = null
            ))
        } else {
            handler2.onFailure(null)
        }
    }

    override suspend fun call(
        request: AckParams, handler: IResponseHandler<String, IResponse<String>>?
    ) {
        handler?.onResponse(chatApiService.acknowledgeMessage(request))
    }

    private fun getSupportData(messages: List<Message>): AckParams {
        val sortedList = messages.sortedBy { it.messageTimestamp }
        return AckParams(
            headers = AckParams.Headers(token = ""),
            params = AckParams.Params(
                from = sortedList.first().messageTimestamp!!,
                to = sortedList.last().messageTimestamp!!,
                chatReference = "",
                yourUsername = ""
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