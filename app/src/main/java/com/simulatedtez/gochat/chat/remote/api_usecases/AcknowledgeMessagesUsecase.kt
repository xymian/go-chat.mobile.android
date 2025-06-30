package com.simulatedtez.gochat.chat.remote.api_usecases

import com.simulatedtez.gochat.chat.remote.models.AckResponse
import com.simulatedtez.gochat.chat.remote.models.Message
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import utils.ChatEndpointCaller
import java.io.IOException

class AcknowledgeMessagesUsecase: ChatEndpointCaller<List<Message>, AckResponse> {

    private val client: OkHttpClient = OkHttpClient()

    override suspend fun call(data: List<Message>?, handler: ChatEndpointCaller.ResponseCallback<AckResponse>) {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        data?.let { messages ->
            val sortedMessages = messages.sortedBy { it.messageTimestamp }
            val requestBody = getAckRequestBuilder(
                from = sortedMessages.first(), to = sortedMessages.last()).toRequestBody(mediaType)
            client.newCall(
                Request.Builder().url("")
                .post(requestBody).build())
                .enqueue(object: Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        handler.onFailure(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val res = AckResponse(
                            data = response.body.toString(),
                            isSuccessful = response.isSuccessful,
                            error = response.message
                        )
                        handler.onResponse(res)
                    }
                })
        }
    }

    private fun getAckRequestBuilder(from: Message, to: Message): String {
        return """
                {
                    "username":${from.senderUsername},
                    "chatReference":${from.chatReference},
                    "from":${from.messageTimestamp},
                    "to":${to.messageTimestamp}
                }
            """.trimIndent()
    }
}