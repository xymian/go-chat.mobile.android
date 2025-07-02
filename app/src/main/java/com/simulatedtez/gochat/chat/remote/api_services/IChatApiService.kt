package com.simulatedtez.gochat.chat.remote.api_services

import com.simulatedtez.gochat.chat.remote.api_usecases.AcknowledgeMessagesUsecase.AckParams
import com.simulatedtez.gochat.chat.remote.api_usecases.GetMissingMessagesUsecase.GetMissingMessagesParams
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.remote.IResponse

interface IChatApiService {
    suspend fun getMissingMessages(params: GetMissingMessagesParams): IResponse<List<Message>>
    suspend fun acknowledgeMessage(params: AckParams): IResponse<String>
}