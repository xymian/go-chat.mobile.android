package com.simulatedtez.gochat.chat.remote.api_services

import com.simulatedtez.gochat.chat.remote.api_usecases.CreateChatRoomParams
import com.simulatedtez.gochat.conversations.remote.api_usecases.CreateConversationsParams
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.ParentResponse

interface IChatApiService {
    suspend fun createChatRoom(params: CreateChatRoomParams): IResponse<ParentResponse<String>>
    suspend fun createConversations(params: CreateConversationsParams): IResponse<ParentResponse<String>>
}