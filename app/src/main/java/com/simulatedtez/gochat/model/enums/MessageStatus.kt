package com.simulatedtez.gochat.model.enums

enum class MessageStatus {
    TYPING, NOT_TYPING, SENDING, SENT, NOT_SENT, DELIVERED, SEEN, FAILED;

    companion object {
        fun getType(value: String?): MessageStatus? {
            return when(value) {
                TYPING.name -> TYPING
                NOT_TYPING.name -> NOT_TYPING
                SENDING.name -> SENDING
                SENT.name -> SENT
                NOT_SENT.name -> NOT_SENT
                DELIVERED.name -> DELIVERED
                SEEN.name -> SEEN
                FAILED.name -> FAILED
                else -> null
            }
        }
    }
}