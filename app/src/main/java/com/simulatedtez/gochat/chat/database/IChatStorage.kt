package com.simulatedtez.gochat.chat.database

import com.simulatedtez.gochat.chat.remote.models.Message

interface IChatStorage {
    suspend fun loadMessages(page: Int, size: Int): List<Message>
    suspend fun storeMessage(message: Message)
    suspend fun storeMessages(messages: List<Message>)
}