package com.simulatedtez.gochat.conversations.remote.api_services

import com.simulatedtez.gochat.conversations.remote.api_usecases.StartNewChatParams
import com.simulatedtez.gochat.conversations.remote.models.NewChatResponse
import com.simulatedtez.gochat.remote.IResponse

interface IConversationsService {

    suspend fun addNewConversation(params: StartNewChatParams): IResponse<NewChatResponse>
}