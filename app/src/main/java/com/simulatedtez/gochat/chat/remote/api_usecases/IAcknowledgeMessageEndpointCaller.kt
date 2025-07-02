package com.simulatedtez.gochat.chat.remote.api_usecases

import com.simulatedtez.gochat.chat.remote.models.AckResponse
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.remote.IEndpointCaller
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import utils.ChatEndpointCaller

interface IAcknowledgeMessageEndpointCaller: ChatEndpointCaller<List<Message>, AckResponse>,
    IEndpointCaller<AcknowledgeMessagesUsecase.AckParams, String, IResponse<String>> {

    override suspend fun call(data: List<Message>?, handler: ChatEndpointCaller.ResponseCallback<AckResponse>)

    suspend fun call(
        request: AcknowledgeMessagesUsecase.AckParams,
        data: List<Message>?,
        handler1: IResponseHandler<String, IResponse<String>>,
        handler2: ChatEndpointCaller.ResponseCallback<AckResponse>)
}